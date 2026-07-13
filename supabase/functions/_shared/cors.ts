// The Edge Function is only ever called from the Nudge Android app via the Supabase client SDK,
// not from a browser, but Supabase's function gateway still runs a CORS preflight — handle it
// so `supabase functions serve` / local testing via curl-from-browser doesn't 405.
export const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers":
    "authorization, x-client-info, apikey, content-type",
};
