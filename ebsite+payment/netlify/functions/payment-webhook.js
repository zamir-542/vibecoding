// ============================================================
// payment-webhook.js — Netlify Function
// Menerima Callback URL dari ToyyibPay, mengesahkan pembayaran,
// dan menghantar e-book ke e-mel pembeli melalui Resend API.
// ============================================================
//
// Callback URL untuk diisi di ToyyibPay:
//   https://[nama-site-anda].netlify.app/.netlify/functions/payment-webhook
//   ATAU (jika guna redirect dalam netlify.toml):
//   https://[nama-site-anda].netlify.app/api/payment-webhook
// ============================================================

const { Resend } = require("resend");

// Parse application/x-www-form-urlencoded body
// (ToyyibPay menghantar data dalam format ini)
function parseFormBody(body) {
  const params = {};
  if (!body) return params;
  const pairs = body.split("&");
  for (const pair of pairs) {
    const [key, value] = pair.split("=");
    if (key) {
      params[decodeURIComponent(key)] = decodeURIComponent(
        (value || "").replace(/\+/g, " ")
      );
    }
  }
  return params;
}

// Template HTML e-mel yang dihantar kepada pembeli
function buildEmailHtml({ name, ebookUrl }) {
  const displayName = name || "Pelanggan";
  return `
<!DOCTYPE html>
<html lang="ms">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>E-book Anda Sudah Sedia</title>
</head>
<body style="margin:0;padding:0;background:#0a0a0a;font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" style="background:#0a0a0a;padding:40px 0;">
    <tr>
      <td align="center">
        <table width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;background:#1a1a1a;border-radius:16px;overflow:hidden;border:1px solid rgba(249,115,22,0.3);">

          <!-- Header -->
          <tr>
            <td style="background:linear-gradient(135deg,#f97316,#ea580c);padding:32px;text-align:center;">
              <p style="margin:0;font-size:2rem;">🎨</p>
              <h1 style="margin:8px 0 0;color:#ffffff;font-size:1.4rem;font-weight:800;letter-spacing:-0.02em;">
                Pembelian Berjaya!
              </h1>
            </td>
          </tr>

          <!-- Body -->
          <tr>
            <td style="padding:40px 36px;">
              <p style="margin:0 0 16px;color:#e5e5e5;font-size:1rem;line-height:1.6;">
                Salam <strong style="color:#f97316;">${displayName}</strong>,
              </p>
              <p style="margin:0 0 16px;color:#a3a3a3;font-size:0.95rem;line-height:1.7;">
                Terima kasih kerana membeli <strong style="color:#e5e5e5;">Belajar Melukis Semudah ABC</strong>.
                Kami sangat gembira menyambut anda sebagai sebahagian daripada komuniti MelukisABC!
              </p>
              <p style="margin:0 0 28px;color:#a3a3a3;font-size:0.95rem;line-height:1.7;">
                Sila muat turun e-book anda di pautan di bawah. Pautan ini adalah milik anda seumur hidup.
              </p>

              <!-- CTA Button -->
              <table width="100%" cellpadding="0" cellspacing="0">
                <tr>
                  <td align="center">
                    <a href="${ebookUrl}"
                       style="display:inline-block;background:#f97316;color:#ffffff;text-decoration:none;
                              font-weight:700;font-size:1rem;padding:16px 40px;border-radius:12px;
                              letter-spacing:0.02em;">
                      📥 Muat Turun E-book Sekarang
                    </a>
                  </td>
                </tr>
              </table>

              <p style="margin:28px 0 0;color:#666;font-size:0.78rem;line-height:1.6;text-align:center;">
                Jika butang di atas tidak berfungsi, salin dan tampal pautan ini ke pelayar anda:<br/>
                <a href="${ebookUrl}" style="color:#f97316;word-break:break-all;">${ebookUrl}</a>
              </p>
            </td>
          </tr>

          <!-- Footer -->
          <tr>
            <td style="background:#111;padding:20px 36px;border-top:1px solid rgba(255,255,255,0.05);">
              <p style="margin:0;color:#444;font-size:0.75rem;text-align:center;">
                © 2025 MelukisABC · Semua hak cipta terpelihara<br/>
                E-mel ini dihantar secara automatik selepas pembayaran disahkan.
              </p>
            </td>
          </tr>

        </table>
      </td>
    </tr>
  </table>
</body>
</html>
  `.trim();
}

// ---- MAIN HANDLER ----
exports.handler = async function (event) {
  // 1. Hanya terima POST request
  if (event.httpMethod !== "POST") {
    return {
      statusCode: 405,
      body: JSON.stringify({ status: "error", message: "Method Not Allowed" }),
    };
  }

  // 2. Parse body dari ToyyibPay
  const params = parseFormBody(event.body);

  console.log("[webhook] Payload diterima dari ToyyibPay:", params);

  // 3. Semak status pembayaran
  //    status_id = 1 → Berjaya
  //    status_id = 2 → Gagal / Belum bayar
  //    status_id = 3 → Pending
  const statusId = params.status_id;

  if (statusId !== "1") {
    console.log(`[webhook] Pembayaran tidak berjaya. status_id=${statusId}. Diabaikan.`);
    // Pulangkan 200 kepada ToyyibPay supaya ia tidak retry
    return {
      statusCode: 200,
      body: JSON.stringify({
        status: "ignored",
        message: "Payment not successful",
      }),
    };
  }

  // 4. Ekstrak maklumat pembeli
  const buyerEmail = params.email;
  const buyerName  = params.name || "Pelanggan";
  const orderId    = params.order_id || params.billcode || "N/A";

  if (!buyerEmail) {
    console.error("[webhook] E-mel pembeli tidak ditemui dalam payload.");
    return {
      statusCode: 400,
      body: JSON.stringify({ status: "error", message: "Email not found in payload" }),
    };
  }

  // 5. Dapatkan nilai dari Environment Variables
  const resendApiKey = process.env.RESEND_API_KEY;
  const fromEmail    = process.env.FROM_EMAIL || "noreply@resend.dev";
  const ebookUrl     = process.env.EBOOK_DOWNLOAD_URL || "https://example.com/ebook.pdf";

  if (!resendApiKey) {
    console.error("[webhook] RESEND_API_KEY tidak dijumpai dalam environment variables!");
    return {
      statusCode: 500,
      body: JSON.stringify({ status: "error", message: "Server configuration error" }),
    };
  }

  // 6. Hantar e-mel via Resend
  try {
    const resend = new Resend(resendApiKey);

    const { data, error } = await resend.emails.send({
      from:    fromEmail,
      to:      buyerEmail,
      subject: "🎨 E-book Anda Sudah Sedia — Belajar Melukis Semudah ABC",
      html:    buildEmailHtml({ name: buyerName, ebookUrl }),
    });

    if (error) {
      console.error("[webhook] Resend error:", error);
      return {
        statusCode: 500,
        body: JSON.stringify({ status: "error", message: "Failed to send email", detail: error }),
      };
    }

    console.log(`[webhook] ✅ E-mel berjaya dihantar kepada ${buyerEmail}. Order: ${orderId}. Resend ID: ${data?.id}`);

    return {
      statusCode: 200,
      body: JSON.stringify({
        status: "ok",
        message: "Email sent",
        resend_id: data?.id,
      }),
    };

  } catch (err) {
    console.error("[webhook] Exception semasa menghantar e-mel:", err);
    return {
      statusCode: 500,
      body: JSON.stringify({ status: "error", message: err.message }),
    };
  }
};
