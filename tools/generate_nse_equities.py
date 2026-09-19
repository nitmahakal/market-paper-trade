import csv
import io
import urllib.request
from pathlib import Path


NSE_URL = "https://www.nseindia.com/api/equity-master"
OUTPUT = Path("nse_equities.csv")


def download_nse_file():
    request = urllib.request.Request(
        NSE_URL,
        headers={
            "User-Agent": "Mozilla/5.0",
            "Accept": "text/csv,*/*",
        },
    )

    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read().decode("utf-8-sig")


def main():
    print("Downloading NSE equity master...")

    data = download_nse_file()

    reader = csv.DictReader(io.StringIO(data))

    rows = []

    for row in reader:
        symbol = (row.get("SYMBOL") or "").strip()
        name = (
            row.get("NAME OF COMPANY")
            or row.get("NAME")
            or ""
        ).strip()

        series = (row.get("SERIES") or "").strip().upper()

        if not symbol or not name:
            continue

        # Normal fully-paid equity shares.
        if series and series != "EQ":
            continue

        rows.append(
            {
                "symbol": symbol.upper(),
                "name": name,
            }
        )

    rows.sort(key=lambda x: x["symbol"])

    with OUTPUT.open(
        "w",
        encoding="utf-8",
        newline=""
    ) as file:
        writer = csv.DictWriter(
            file,
            fieldnames=["symbol", "name"]
        )

        writer.writeheader()
        writer.writerows(rows)

    print(f"Created: {OUTPUT}")
    print(f"Stocks: {len(rows)}")


if __name__ == "__main__":
    main()
