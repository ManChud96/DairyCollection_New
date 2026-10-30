DAIRY COLLECTION - Android Studio project

LOGIN: user = admin   password = 1234   (change in Store.kt -> USER / PASS)

1. Install Android Studio (Koala or newer).
2. File > Open > select this DairyCollection folder. Wait for Gradle sync (internet needed the first time).
3. Connect your phone (USB debugging on) and press Run.
4. Copy customers_sample.txt to the phone.

SCREENS
 Login  -> Home (shows today's totals)
 1. Upload Data      : pick customers .txt (Name,CustomerId per line)
 2. Collection Entry : customer, date, morning/evening, liter, fat, snf, degree, rate, total -> Save
 3. Export Data      : choose from/to date, Download Text File -> phone Downloads folder

PRINTING (Bluetooth thermal printer, ESC/POS, 58mm)
 1. Pair the printer in phone Settings > Bluetooth first.
 2. Home > Select Bluetooth Printer > choose it.
 3. Collection Entry > "Save & Print Receipt".
 Shop name on receipt: Store.kt -> SHOP_NAME.  For 80mm paper set W = 48 in Printer.kt.

DAILY REPORT / REPRINT (Home page)
 4. Daily Collection Report : pick a date, see totals (morning / evening / total, average fat and SNF), tap Print Report.
 5. Reprint Receipt         : pick a date, tap an entry, confirm. Prints a receipt marked DUPLICATE COPY.

SMS TO CUSTOMER
 After every Save (or Save & Print) the app asks "Send SMS?" if the customer has a mobile number in the uploaded file.
 Yes = sends through the phone's default SMS SIM. No = nothing is sent. SMS permission is asked the first time.

SMS BLOCKED ("denied access")
 Android 13+ blocks SMS permission for APKs installed from a file. Fix once:
 Settings > Apps > Dairy Collection > menu (3 dots, top right) > Allow restricted settings,
 then Permissions > SMS > Allow. Or tap "Open Messages" in the app to send from the Messages app.
