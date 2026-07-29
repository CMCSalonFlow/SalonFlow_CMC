const fs = require('fs');
const path = require('path');

const jsonPath = process.argv[2];
const pdfPath = process.argv[3];

if (!jsonPath || !pdfPath) {
    console.error("Usage: node renderInvoice.js <jsonPath> <pdfPath>");
    process.exit(1);
}

function findChromePath() {
    const candidatePaths = [
        'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe',
        'C:\\Program Files\\Microsoft\\Edge\\Application\\msedge.exe',
        'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',
        'C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe'
    ];
    for (const p of candidatePaths) {
        if (fs.existsSync(p)) {
            return p;
        }
    }
    return null;
}

async function renderPdfWithPuppeteer(invoiceData, outputPath) {
    const puppeteer = require('puppeteer');

    const salonName = invoiceData.salonName || "SalonFlow Hair Salon";
    const salonPhone = invoiceData.salonPhone || "0900000000";
    const salonAddress = invoiceData.salonAddress || "Hà Nội, Việt Nam";
    const customerName = invoiceData.customerName || "Khách hàng";
    const customerPhone = invoiceData.customerPhone || "N/A";
    const bookingId = invoiceData.bookingId || "0";
    const bookingTime = invoiceData.bookingTime ? String(invoiceData.bookingTime).replace('T', ' ').substring(0, 16) : new Date().toLocaleDateString('vi-VN');
    
    const totalVal = invoiceData.total || invoiceData.subTotal || 0;
    const totalFormatted = Number(totalVal).toLocaleString('vi-VN') + " VNĐ";

    let itemsRows = "";
    if (Array.isArray(invoiceData.items) && invoiceData.items.length > 0) {
        invoiceData.items.forEach((item, idx) => {
            const name = item.serviceName || item.name || "Dịch vụ";
            const priceVal = item.totalPrice || item.unitPrice || item.price || 0;
            const price = Number(priceVal).toLocaleString('vi-VN') + " VNĐ";
            itemsRows += `
                <tr>
                    <td style="width: 40px; text-align: center; color: #64748b;">${idx + 1}</td>
                    <td style="font-weight: 600; color: #1e293b;">${name}</td>
                    <td style="text-align: center; color: #475569;">1</td>
                    <td style="text-align: right; font-weight: 600; color: #0f172a;">${price}</td>
                </tr>
            `;
        });
    } else {
        itemsRows = `<tr><td colspan="4" style="text-align:center; color:#94a3b8; padding: 20px;">Không có thông tin dịch vụ</td></tr>`;
    }

    const htmlContent = `
<!DOCTYPE html>
<html lang="vi">
<head>
<meta charset="UTF-8">
<style>
  @import url('https://fonts.googleapis.com/css2?family=Be+Vietnam+Pro:wght@400;500;600;700&display=swap');
  * { box-sizing: border-box; }
  body {
    font-family: 'Be Vietnam Pro', sans-serif, system-ui;
    margin: 0;
    padding: 40px;
    color: #1e293b;
    background: #ffffff;
  }
  .header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    border-bottom: 2px solid #6366f1;
    padding-bottom: 20px;
    margin-bottom: 24px;
  }
  .brand-title {
    font-size: 26px;
    font-weight: 800;
    color: #4f46e5;
    letter-spacing: -0.5px;
  }
  .brand-sub {
    font-size: 13px;
    color: #64748b;
    margin-top: 4px;
  }
  .invoice-badge {
    text-align: right;
  }
  .invoice-title {
    font-size: 22px;
    font-weight: 700;
    color: #0f172a;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }
  .invoice-code {
    display: inline-block;
    background: #e0e7ff;
    color: #4338ca;
    font-size: 13px;
    font-weight: 700;
    padding: 4px 10px;
    border-radius: 6px;
    margin-top: 6px;
  }
  .grid {
    display: flex;
    justify-content: space-between;
    margin-bottom: 28px;
    gap: 20px;
  }
  .box {
    flex: 1;
    background: #f8fafc;
    border-radius: 10px;
    padding: 16px;
    border: 1px solid #e2e8f0;
  }
  .box-title {
    font-size: 11px;
    font-weight: 700;
    text-transform: uppercase;
    color: #64748b;
    letter-spacing: 0.8px;
    margin-bottom: 8px;
  }
  .box p {
    margin: 4px 0;
    font-size: 13px;
    color: #334155;
  }
  .table {
    width: 100%;
    border-collapse: collapse;
    margin-bottom: 28px;
  }
  .table th {
    background: #f1f5f9;
    padding: 12px 14px;
    font-size: 12px;
    font-weight: 700;
    text-transform: uppercase;
    color: #475569;
    border-bottom: 2px solid #cbd5e1;
  }
  .table td {
    padding: 14px;
    font-size: 13px;
    border-bottom: 1px solid #e2e8f0;
  }
  .total-container {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 36px;
  }
  .total-card {
    width: 300px;
    background: #4f46e5;
    color: #ffffff;
    padding: 16px 20px;
    border-radius: 10px;
    box-shadow: 0 4px 12px rgba(79, 70, 229, 0.2);
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .total-label {
    font-size: 13px;
    font-weight: 600;
    text-transform: uppercase;
    opacity: 0.9;
  }
  .total-val {
    font-size: 18px;
    font-weight: 800;
  }
  .footer {
    text-align: center;
    border-top: 1px solid #e2e8f0;
    padding-top: 20px;
    font-size: 13px;
    color: #64748b;
  }
  .thank-you {
    font-weight: 600;
    color: #4338ca;
    margin-bottom: 4px;
  }
</style>
</head>
<body>
  <div class="header">
    <div>
      <div class="brand-title">${salonName}</div>
      <div class="brand-sub">Hệ thống Quản lý & Đặt lịch SalonFlow</div>
    </div>
    <div class="invoice-badge">
      <div class="invoice-title">HÓA ĐƠN THANH TOÁN</div>
      <div class="invoice-code">MÃ ĐƠN: #BK-${bookingId}</div>
    </div>
  </div>

  <div class="grid">
    <div class="box">
      <div class="box-title">THÔNG TIN SALON</div>
      <p><strong>Cơ sở:</strong> ${salonName}</p>
      <p><strong>Địa chỉ:</strong> ${salonAddress}</p>
      <p><strong>Hotline:</strong> ${salonPhone}</p>
    </div>
    <div class="box">
      <div class="box-title">THÔNG TIN KHÁCH HÀNG</div>
      <p><strong>Họ tên:</strong> ${customerName}</p>
      <p><strong>Số điện thoại:</strong> ${customerPhone}</p>
      <p><strong>Thời gian làm:</strong> ${bookingTime}</p>
    </div>
  </div>

  <table class="table">
    <thead>
      <tr>
        <th style="width: 40px; text-align: center;">STT</th>
        <th style="text-align: left;">Dịch Vụ / Gói Sử Dụng</th>
        <th style="width: 80px; text-align: center;">SL</th>
        <th style="width: 140px; text-align: right;">Thành Tiền</th>
      </tr>
    </thead>
    <tbody>
      ${itemsRows}
    </tbody>
  </table>

  <div class="total-container">
    <div class="total-card">
      <div class="total-label">TỔNG THÀNH TIỀN</div>
      <div class="total-val">${totalFormatted}</div>
    </div>
  </div>

  <div class="footer">
    <div class="thank-you">Cảm ơn quý khách đã tin tưởng và sử dụng dịch vụ tại ${salonName}!</div>
    <div>Mọi thắc mắc xin vui lòng liên hệ hotline ${salonPhone} để được hỗ trợ.</div>
  </div>
</body>
</html>
    `;

    const launchOptions = {
        headless: "new",
        args: ['--no-sandbox', '--disable-setuid-sandbox']
    };

    const localChrome = findChromePath();
    if (localChrome) {
        launchOptions.executablePath = localChrome;
    }

    const browser = await puppeteer.launch(launchOptions);
    const page = await browser.newPage();
    await page.setContent(htmlContent, { waitUntil: 'networkidle0' });
    
    fs.mkdirSync(path.dirname(outputPath), { recursive: true });
    await page.pdf({
        path: outputPath,
        format: 'A4',
        printBackground: true,
        margin: { top: '20px', bottom: '20px', left: '20px', right: '20px' }
    });

    await browser.close();
}

async function main() {
    try {
        const rawData = fs.readFileSync(jsonPath, 'utf8');
        const invoiceData = JSON.parse(rawData);

        await renderPdfWithPuppeteer(invoiceData, pdfPath);
        console.log("PDF generated successfully with Puppeteer UTF-8:", pdfPath);
        process.exit(0);
    } catch (err) {
        console.error("Puppeteer PDF generation failed:", err);
        process.exit(1);
    }
}

main();