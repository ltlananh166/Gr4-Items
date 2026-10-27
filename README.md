# Flash Sale System

Java console application implementing the use cases in `docs/UsecaseDiagram.drawio.png`.

## Run

Requires a JDK. From the repository root in PowerShell:

```powershell
$sources = Get-ChildItem src\main\java -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d out $sources
java -cp out view.Main
```

To generate the sample CSV dataset, run:

```powershell
java -cp out view.Main --generate-data
```

This creates 5,000 products, 2,000 sample customers, 10 vouchers, 10 Flash Sale events, 500 Flash Sale items, 2,500 orders, and 2,500 order details under `data\`. Existing customer rows are preserved and sample customers are appended with unique IDs. Existing voucher rows are preserved; generated vouchers are added only when their codes are missing. Other existing CSV files are left untouched. Passwords are stored as salted PBKDF2 hashes. The administrator and researcher menus are available as local console roles; this prototype does not implement role authentication.

## Use cases

- **Guest:** register, log in, browse regular products and Flash Sale events, search products, and view product details.
- **Customer:** browse and search, add products to a cart, view the cart, preview/apply a voucher at checkout, place and cancel unpaid orders, pay through the simulated VNPay option or wallet, and view order and transaction history.
- **Administrator:** create, view, update, and delete products and users; ban/unban users; adjust inventory; create/update/end/cancel Flash Sales and assign products; manage vouchers; approve/cancel orders; and view a sales report.
- **Researcher:** configure concurrent thread count and locking mechanism, run a Flash Sale concurrency simulation or benchmark all lock mechanisms, view the latest results, and export simulation records as CSV.

The application loads its regular products and initial Flash Sale in memory at startup. Customer accounts and vouchers are stored in CSV files; the generated product, event, item, order, and detail files are sample datasets and are not loaded into the in-memory catalog.
