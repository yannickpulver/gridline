// Instagram Login for Gridline.
//
// 1. App opens  /instagram-auth/login?state=<uuid>   → redirects to Instagram
// 2. Instagram → /instagram-auth/callback?code&state → long-lived token stored under state
// 3. App polls  /instagram-auth/token?state=<uuid>   → gets the token once, row is deleted
//
// The app secret only lives here; the app itself never needs it.

import { createClient } from "npm:@supabase/supabase-js@2.49.4";

const APP_ID = Deno.env.get("INSTAGRAM_APP_ID")!;
const APP_SECRET = Deno.env.get("INSTAGRAM_APP_SECRET")!;
const CALLBACK_URL = `${Deno.env.get("SUPABASE_URL")}/functions/v1/instagram-auth/callback`;
const LOGIN_TTL_MINUTES = 10;

const db = createClient(
  Deno.env.get("SUPABASE_URL")!,
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
);

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

Deno.serve(async (req) => {
  const url = new URL(req.url);
  const state = url.searchParams.get("state") ?? "";
  if (!UUID.test(state)) return text("Invalid state", 400);

  if (url.pathname.endsWith("/login")) return login(state);
  if (url.pathname.endsWith("/callback")) return callback(state, url.searchParams);
  if (url.pathname.endsWith("/token")) return token(state);
  return text("Not found", 404);
});

function login(state: string) {
  const authorize = new URL("https://www.instagram.com/oauth/authorize");
  authorize.searchParams.set("client_id", APP_ID);
  authorize.searchParams.set("redirect_uri", CALLBACK_URL);
  authorize.searchParams.set("response_type", "code");
  authorize.searchParams.set("scope", "instagram_business_basic");
  authorize.searchParams.set("enable_fb_login", "false");
  authorize.searchParams.set("state", state);
  return Response.redirect(authorize.toString(), 302);
}

async function callback(state: string, params: URLSearchParams) {
  const code = params.get("code");
  if (!code) return text(`Instagram login cancelled: ${params.get("error_description") ?? "no code"}`, 400);

  const shortLived = await fetch("https://api.instagram.com/oauth/access_token", {
    method: "POST",
    body: new URLSearchParams({
      client_id: APP_ID,
      client_secret: APP_SECRET,
      grant_type: "authorization_code",
      redirect_uri: CALLBACK_URL,
      code: code.replace(/#_$/, ""),
    }),
  }).then((r) => r.json());
  // The docs wrap the response in `data`; accept the flat shape as well
  const shortToken = (shortLived.data?.[0] ?? shortLived).access_token;
  if (!shortToken) {
    console.error("Code exchange failed", shortLived);
    return text("Instagram login failed. Please try again in Gridline.", 502);
  }

  const exchange = new URL("https://graph.instagram.com/access_token");
  exchange.searchParams.set("grant_type", "ig_exchange_token");
  exchange.searchParams.set("client_secret", APP_SECRET);
  exchange.searchParams.set("access_token", shortToken);
  const longLived = await fetch(exchange).then((r) => r.json());
  if (!longLived.access_token) {
    console.error("Long-lived exchange failed", longLived);
    return text("Instagram login failed. Please try again in Gridline.", 502);
  }

  const { error } = await db.from("instagram_logins").upsert({
    state,
    access_token: longLived.access_token,
    expires_in: longLived.expires_in,
  });
  if (error) {
    console.error("Storing token failed", error);
    return text("Instagram login failed. Please try again in Gridline.", 500);
  }

  return text("Logged in with Instagram. You can go back to Gridline now.");
}

async function token(state: string) {
  const cutoff = new Date(Date.now() - LOGIN_TTL_MINUTES * 60_000).toISOString();
  await db.from("instagram_logins").delete().lt("created_at", cutoff);

  const { data } = await db.from("instagram_logins")
    .delete()
    .eq("state", state)
    .select("access_token, expires_in")
    .maybeSingle();
  if (!data) return text("Pending", 404);

  return Response.json(data);
}

function text(body: string, status = 200) {
  return new Response(body, { status, headers: { "Content-Type": "text/plain; charset=utf-8" } });
}
