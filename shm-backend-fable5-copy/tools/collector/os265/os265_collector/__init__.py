"""OS265 file collector package.

Reads OS265 vendor record files and forwards normalized vendor readings
to the backend's unified vendor source layer.

Module layout:
    config         constants and CLI argument parsing
    log_setup      logging configuration
    state_manager  offset/posted-key state persistence
    line_parser    channel file line parsing (decode, time, decimals)
    record_model   ParsedRecord model and canonical payload builder
    file_discovery candidate file selection and tail reading
    upload_client  HTTP upload (canonical first, legacy fallback) and verify
    scan_loop      single scan pass and the long-running loop
"""
