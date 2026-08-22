# Store-Item external history import

Maps `item_1..item_50` to `SP001..SP050` and `store_1..store_3` to `K001..K003`.

Run through the Spring application runner with `--store-item-import --dry-run=false`.

The importer writes only `ai.lich_su_ban_hang` rows with `nguon_du_lieu='EXTERNAL_STORE_ITEM'`.
It does not create operational inventory, receipts, approvals, counts, employees, partners, or warehouse capacity data.
