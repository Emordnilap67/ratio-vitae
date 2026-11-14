# Ratio Vitae

**Your money and your health, in one app, on your phone - and nowhere else.**

Ratio Vitae ("the reckoning of a life") is a budgeting app and a medication
tracker in one. It started as one person's own app, built to replace a money
manager and a pile of spreadsheets, and this is that same app with every
feature - and none of his data. It starts as a **blank slate**: you add your
own accounts, categories, bills, cards, investments and medications, and it
becomes yours.

- **Free**, no ads, no account, no sign-in.
- **Almost no internet.** The only thing it ever looks up is share prices over time for the
  investments you hold (Yahoo Finance's daily closes) - only the ticker name goes out. Nothing
  about you is sent anywhere.
- **Your data is plain files you own** - simple csv files in a folder called
  `Ratio Vitae` on your phone. Open them in any spreadsheet, back them up,
  sync them, move them to a new phone.

## A quick tour

Five pages - swipe between them.

**Budget**
- *Left now* - what came in minus what has gone out, at a glance, plus your
  main account, cash and other accounts.
- *Coming up* - every bill still to pay on its day, what your account will
  have after each one, and your **lowest point** before the next payday.
- The **+** button: I spent, Money in, Move money between accounts, a card
  purchase, a card payment, cash, money you borrowed, your overdraft cover or
  credit line. Several items with their own tax, groceries and takeout,
  "same as last time".
- Tap any line to pay it, change it or take it off. Tap a past entry to
  delete it or mark it "not out yet". A later date = a transaction still to
  come.
- A new month: one tap carries your balance, income and fixed and monthly
  bills over, unpaid.
- Cash back and interest totals, subscriptions with their yearly total, a
  grocery list with the prices you last paid, and a **PDF of the month**.

**Stats** - your own categories and subcategories (with icons), spending by
month, quarter or year as rings and bars, and a search.

**Credit** - your credit scores (Equifax and TransUnion) over time, every card
and loan with its rate, minimum and due date, each loan payment split into
principal and interest, payoff dates at your current payments, which debt to
pay first, and buy now / pay later plans.

**Invest** - net worth (and a line of it over time), any investment account
(TFSA, RRSP, FHSA, crypto, anything), goals with a target and a date, and an
optional import of a TD Direct Investing holdings export (prices, shares,
cash and the USD rate).

**Health** - your medications as **morning and night sets** with one-tap
*Take all*, half tablets, injections every N days, what is left in each
bottle and when to order, refills and their cost, recounts for a dropped
pill; weight, blood pressure and pulse, temperature (°C, °F or K), a food
stock counter, and a **Health PDF** for your doctor.

**Also**
- **Two home-screen widgets**, both scrolling: *Money* (left now, the four
  quick buttons, the next bill, the lowest point) and *Pills* (both sets with
  big Take buttons, blood pressure, heart rate, temperature, what to order).
- **Notifications**: bills the day before, a daily pills reminder, and a note
  each time a set is logged.
- **Read it for me**: a photo or screenshot of a receipt fills in the
  purchase; a card statement (PDF - Triangle, BMO, Capital One, Simplii) or
  a screenshot of a loan's app fills in that card or loan; share a file to
  Ratio Vitae or open it with Ratio Vitae.

## Install

1. Download `RatioVitae-<version>.apk` from the
   [Releases](../../releases/latest) page on your Android phone (Android 8 or
   newer).
2. Open it. Android asks to allow installing from your browser or Files app -
   allow it, go back, tap **Install**.
3. Open Ratio Vitae. It asks for **All files access** - this is only so it can
   keep its files in the `Ratio Vitae` folder where you can see them. Allow it.
4. The Budget page says **Welcome**: type your main account, what it has now,
   and (if you like) your pay. Everything else you add as you go with **+**.

Updates: download the newer APK and install it over the old one - your files
stay where they are.

## Your files

Everything lives in `Ratio Vitae/` on your phone's storage, one csv per kind
of thing - `budget_log.csv`, `cards.csv`, `categories.csv`, `pills_log.csv`,
`pills_taken.csv`, `investments.csv` and so on. The app makes them (empty,
just their column names) the first time it opens, adds rows as you use it,
and never deletes a file. They are yours: copy the folder to back it up.

## Building it yourself

The app is one web page (`assets/index.html` - HTML, CSS and plain
JavaScript) inside a small Android shell (`src/`, Java). GitHub builds the APK
on every version tag (`.github/workflows/release.yml`, `tools/build-ci.sh`);
on any Linux PC with the Android SDK, `bash tools/build-ci.sh` does the same.

## Licence

GPL-3.0 - see `LICENSE`. Inside the app: the Inter font (SIL Open Font
License), pdf.js and Tesseract (Apache 2.0) - their licences are in
`assets/`.

Made by Emordnilap67.
