# SYNTHETIC demo tools

These files surround the unchanged application code with a small reproducible demonstration. They contain no measurements from equipment or customers.

From the repository root:

```sh
python tools/demo/generate_synthetic.py
python -m unittest discover -s tools/demo -p "test_*.py" -v
node --test tools/demo/test_chart.mjs
```

- `generate_synthetic.py`: six deterministic records; the default output is the collector's `sample-data/os265` directory. The filename carries SYNTHETIC; no header shifts the source line numbers.
- `test_demo.py`: existing collector behavior exercised in isolation with a temporary loopback HTTP recorder; no MySQL or Java backend.
- `verify_api.py`: read-only API assertions against an already-running, dedicated loopback demo backend. This script does not seed a database or make up responses.
- `test_verify_api.py`: rejection cases for the verifier itself; passing these is not an integration result.
- `test_chart.mjs`: real frontend value/chart helpers exercised under Node; no browser rendering claim.

Generated inputs and runtime state are ignored by Git. The generator refuses to replace a file with different contents. Follow the [full runbook](../../docs/runbook.md) for database setup, real ingestion and the expected Vue curve, and [validation](../../docs/validation.md) for executed evidence.
