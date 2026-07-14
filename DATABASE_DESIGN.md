# سند طراحی دیتابیس بومی SQLite (Room Database)
## Persian Invoice & Proforma Database Schema

این سند حاوی معماری گام‌به‌گام و جامع دیتابیس آفلاین اپلیکیشن **«فاکتور دفتری»** می‌باشد که با زبان SQLite و مناسب برای نگاشت مستقیم در کتابخانه Room شبیه‌سازی گردیده است.

---

## ۱. دیاگرام روابط جداول (ERD Simplified Schema)

- **دسته بندی به محصول (Categories ➡️ Products)**: رابطه یک‌به‌چند (هر محصول یک دسته‌بندی دارد).
- **مشتری به فاکتور (Customers ➡️ Invoices)**: رابطه یک‌به‌چند (هر فاکتور متعلق به یک مشتری است).
- **فاکتور به اقلام (Invoices ➡️ Invoice Items)**: رابطه یک‌به‌چند با رفتار `ON DELETE CASCADE` (با حذف فاکتور، اقلام مربوطه حذف می‌شوند).
- **فاکتور به پرداخت‌ها (Invoices ➡️ Payments)**: رابطه یک‌به‌چند با رفتار `ON DELETE CASCADE` (با حذف فاکتور، تمام گردش‌های مالی پیوسته ملغی می‌گردند).
- **پروفایل شرکت و تنظیمات (Company Info & Settings)**: جداول تک‌ردیفه (Singleton) با کلید اصلی ثابت `1` جهت اعمال ساختگی.

---

## ۲. تعریف دقیق جداول و فیلدها (SQL DDL)

### ۱. جدول دسته‌بندی محصولات (`categories`)
جهت دسته‌بندی موضوعی کالاها و خدمات.

```sql
CREATE TABLE categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
);

-- ایندکس جهت جستجوی سریع دسته بندی‌ها
CREATE UNIQUE INDEX idx_category_name ON categories(name);
```

### ۲. جدول محصولات و کالاها (`products`)
شامل اطلاعات کالا یا خدمات فروشگاهی به همراه بارکد مستقل.

```sql
CREATE TABLE products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    category_id INTEGER,
    name TEXT NOT NULL,
    description TEXT,
    unit TEXT NOT NULL DEFAULT 'عدد', -- کیلوگرم، متر، ساعت، جعبه و غیره
    default_unit_price REAL NOT NULL DEFAULT 0.0,
    barcode TEXT,
    tax_rate REAL NOT NULL DEFAULT 0.0, -- درصد مالیات پیش فرض کالا (مانند 10.0)
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
    FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL
);

-- ایندکس‌ها جهت اسکن سریع بارکد و جستجوی متنی محصول
CREATE INDEX idx_product_barcode ON products(barcode);
CREATE INDEX idx_product_name ON products(name);
```

### ۳. جدول مشتریان (`customers`)
ثبت اطلاعات افراد حقیقی یا حقوقی.

```sql
CREATE TABLE customers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name TEXT NOT NULL,
    phone_number TEXT NOT NULL,
    national_id TEXT, -- کد ملی یا شناسه ملی شرکت
    economic_code TEXT, -- کد اقتصادی برای خریدهای رسمی
    address TEXT,
    postal_code TEXT,
    email TEXT,
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
);

-- ایندکس‌ها برای جستجو بر اساس نام و شماره تلفن مشتری
CREATE INDEX idx_customer_full_name ON customers(full_name);
CREATE INDEX idx_customer_phone ON customers(phone_number);
```

### ۴. جدول فاکتور اصلی (`invoices`)
این جدول هم فاکتورهای رسمی و هم پیش‌فاکتورها را مدیریت می‌کند.

```sql
CREATE TABLE invoices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER NOT NULL,
    invoice_number TEXT NOT NULL UNIQUE, -- شماره فاکتور یکتا (فرمت سفارشی)
    issue_date INTEGER NOT NULL, -- تاریخ صدور شمسی به صورت Timestamp میلی‌ثانیه‌ای
    due_date INTEGER, -- تاریخ سررسید پرداخت اختیاری
    invoice_type TEXT NOT NULL CHECK(invoice_type IN ('INVOICE', 'PROFORMA')), -- نوع: فاکتور یا پیش‌فاکتور
    status TEXT NOT NULL CHECK(status IN ('DRAFT', 'UNPAID', 'PARTIALLY_PAID', 'PAID', 'CANCELLED')) DEFAULT 'DRAFT',
    discount_amount REAL NOT NULL DEFAULT 0.0, -- تخفیف کلی فاکتور (مبلغ عددی نهایی)
    tax_rate REAL NOT NULL DEFAULT 0.0, -- نرخ مالیات بر ارزش افزوده کلی (مثلا 10.0 درصد)
    shipping_fee REAL NOT NULL DEFAULT 0.0, -- هزینه ارسال یا ایاب ذهاب
    notes TEXT, -- توضیحات و بندهای فاکتور
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
    FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE RESTRICT
);

-- ایندکس‌ها جهت فیلترینگ سریع وضعیت‌ها، نوع فاکتور و مشتری
CREATE INDEX idx_invoice_number ON invoices(invoice_number);
CREATE INDEX idx_invoice_type_status ON invoices(invoice_type, status);
CREATE INDEX idx_invoice_customer ON invoices(customer_id);
```

### ۵. جدول اقلام فاکتور (`invoice_items`)
ارتباط یک‌به‌چند از کالاها به فاکتور صادر شده.

```sql
CREATE TABLE invoice_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_id INTEGER NOT NULL,
    product_id INTEGER, -- اختیاری (در صورت حذف کالا از کاتالوگ، مقدار تهی می‌شود ولی قلم فاکتور حفظ خواهد شد)
    product_name TEXT NOT NULL, -- کپی زنده نام کالا به دلیل امنیت تاریخچه فروش فاکتور
    unit_price REAL NOT NULL, -- قیمت واحد در زمان صدور فاکتور
    unit TEXT NOT NULL, -- واحد شمارش در زمان صدور فاکتور
    quantity REAL NOT NULL, -- تعداد به صورت اعشاری جهت پشتیبانی از مقادیر وزنی/متری
    discount_amount REAL NOT NULL DEFAULT 0.0, -- مبلغ تخفیف خطی این قلم
    tax_amount REAL NOT NULL DEFAULT 0.0, -- مبلغ مالیات محاسبه شده این قلم
    FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE SET NULL
);

CREATE INDEX idx_item_invoice_id ON invoice_items(invoice_id);
```

### ۶. جدول پرداخت‌ها (`payments`)
برای ثبت و رهگیری مبالغ واریز شده و چک‌های دریافتی برای هر فاکتور.

```sql
CREATE TABLE payments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    invoice_id INTEGER NOT NULL,
    amount REAL NOT NULL, -- مبلغ پرداخت شده به ریال/تومان
    payment_date INTEGER NOT NULL, -- زمان پرداخت
    payment_method TEXT NOT NULL CHECK(payment_method IN ('CASH', 'CARD', 'POS', 'CHEQUE', 'SHABA')), -- روش پرداخت
    reference_number TEXT, -- شماره پیگیری یا شماره فیش بانکی/شماره چک
    notes TEXT, -- یادداشت‌های پرداخت (مثلا چک ثبت‌شده صیادی صادرکننده)
    created_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
    FOREIGN KEY (invoice_id) REFERENCES invoices (id) ON DELETE CASCADE
);

CREATE INDEX idx_payment_invoice_id ON payments(invoice_id);
```

### ۷. جدول مشخصات شرکت صادرکننده / اصناف (`company_info`)
این جدول همواره دارای یک ردیف است (`id = 1`) و اطلاعات هویت تجاری صادر کننده را ثبت می‌کند.

```sql
CREATE TABLE company_info (
    id INTEGER PRIMARY KEY CHECK (id = 1), -- تحمیل حالت تک‌ردیفی (Singleton)
    company_name TEXT NOT NULL,
    manager_name TEXT,
    national_id TEXT, -- کد ملی یا شناسه ملی رسمی شرکت
    economic_code TEXT, -- کد اقتصادی صنف
    registration_number TEXT, -- شماره ثبت شرکت
    phone_number TEXT NOT NULL,
    address TEXT,
    postal_code TEXT,
    email TEXT,
    logo_path TEXT, -- آدرس محلی عکس لوگوی برند روی فضای ذخیره تلفن همراه
    signature_path TEXT, -- عکس مهر و امضا
    bank_name TEXT,
    card_number TEXT,
    shaba_number TEXT,
    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
);
```

### ۸. جدول تنظیمات سیستمی اپلیکیشن (`settings`)
مدیریت زبان، گزینه‌های ریال/تومان، درصد پیش‌فرض مالیات و قالب پیش‌فرض رندر PDF.

```sql
CREATE TABLE settings (
    id INTEGER PRIMARY KEY CHECK (id = 1), -- حالت یکتا (Singleton)
    currency_unit TEXT NOT NULL DEFAULT 'تومان' CHECK(currency_unit IN ('تومان', 'ریال')),
    default_tax_rate REAL NOT NULL DEFAULT 0.0, -- نرخ مالیات بر ارزش افزوده پیش‌فرض سراسری صنف
    pdf_template_type TEXT NOT NULL DEFAULT 'OFFICIAL' CHECK(pdf_template_type IN ('OFFICIAL', 'SIMPLE', 'THERMAL_80')), -- نوع پیش‌فرض قالب PDF
    require_biometric_auth INTEGER NOT NULL DEFAULT 0, -- مقدار 0 یعنی غیرفعال، و 1 فعال بودن قفل اثر انگشت دستگاه
    invoice_prefix TEXT NOT NULL DEFAULT 'INV-', -- پیشوند پیش‌فرض شماره‌گذاری خودکار فاکتورها
    updated_at INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
);
```

---

## ۳. روابط و سناریوهای داده‌ای (Relationships & Constraints Analysis)

### ۱. تبدیل پیش‌فاکتور به فاکتور نهایی
از آنجا که هر دو ساختار در جدول واحد `invoices` ذخیره می‌شوند، فرآیند تبدیل بسیار سبک و فاقد پیچیدگی همگام‌سازی است. کافی است مقدار فیلد `invoice_type` از `'PROFORMA'` به `'INVOICE'` تغییر یابد و وضعیت فاکتور به نسبت میزان پرداخت قبلی بروزرسانی شود.

### ۲. پایداری تاریخچه محاسبات (Historical Data Integrity)
با تغییر قیمت پیش‌فرض محصول در جدول `products` یا تغییر نام دسته‌بندی‌ها، فاکتورهای صادر شده قبلی نباید دچار تغییر مالی گردند. به همین منظور فیلدهای `product_name` ،`unit_price` و `unit` به طور زنده در بدنه جدول `invoice_items` کپی و نگهداری می‌شوند.

### ۳. یکپارچگی پرداخت‌ها و محاسبات مانده حساب
فرمول تسویه هر فاکتور به صورت زنده بر اساس مجموع فیلدهای مرتبط محاسبه می‌شود:
$$\text{Total Invoice Amount} = \sum (\text{items.price} \times \text{quantity} - \text{items.discount} + \text{items.tax}) - \text{invoices.discount\_amount} + \text{invoices.shipping\_fee}$$
$$\text{Remaining Debt} = \text{Total Invoice Amount} - \sum (\text{payments.amount})$$

- اگر مجموع مبالغ جدول `payments` برابر صفر باشد ⬅️ وضعیت فاکتور برابر `UNPAID` است.
- اگر مجموع مبالغ بزرگتر از صفر و کوچکتر از قیمت نهایی باشد ⬅️ وضعیت فاکتور برابر `PARTIALLY_PAID` است.
- اگر مجموع مبالغ مساوی قیمت نهایی باشد ⬅️ وضعیت فاکتور برابر `PAID` است.
