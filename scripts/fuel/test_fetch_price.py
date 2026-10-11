import unittest

from fetch_price import parse

PAGE = """<title>Morocco gasoline prices, 05-Oct-2026 | GlobalPetrolPrices.com</title>
<div>The current gasoline price in  Morocco is  MAD 15.18 per liter or USD 1.52 per liter and
was updated on 05-Oct-2026. </div>"""


class ParseTest(unittest.TestCase):
    def test_reads_price_and_date(self):
        d = parse(PAGE)
        self.assertEqual(d["pricePerLiter"], 15.18)
        self.assertEqual(d["asOf"], "2026-10-05")
        self.assertEqual(d["currency"], "MAD")

    def test_layout_change_fails_loudly(self):
        with self.assertRaises(ValueError):
            parse("<html>maintenance</html>")

    def test_rejects_implausible_price(self):
        with self.assertRaises(ValueError):
            parse(PAGE.replace("15.18", "1518"))


if __name__ == "__main__":
    unittest.main()
