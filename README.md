# Flash Sale System

Java console application implementing the use cases in `docs/UsecaseDiagram.drawio.png`.

## Run

Requires a JDK. From the repository root in PowerShell:

```powershell
$sources = Get-ChildItem src\main\java -Recurse -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d out $sources
java -cp out view.Main
```

The application creates `data\customers.csv` and `data\vouchers.csv` when those records are first saved. Passwords are stored as salted PBKDF2 hashes. The administrator and researcher menus are available as local console roles; this prototype does not implement role authentication.

## Use cases

- **Guest:** register, log in, browse regular products and Flash Sale events, search products, and view product details.
- **Customer:** browse and search, add products to a cart, view the cart, preview/apply a voucher at checkout, place and cancel unpaid orders, pay through the simulated VNPay option or wallet, and view order and transaction history.
- **Administrator:** create, view, update, and delete products and users; ban/unban users; adjust inventory; create/update/end/cancel Flash Sales and assign products; manage vouchers; approve/cancel orders; and view a sales report.
- **Researcher:** configure concurrent thread count and locking mechanism, run a Flash Sale concurrency simulation, view the result, and export simulation records as CSV.

Sample products and the initial Flash Sale are loaded in memory at startup. Product, cart, Flash Sale, order, transaction, and simulation state is currently in memory; customer accounts and vouchers are stored in CSV files.
