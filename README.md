# Android Share Intent Diagnostic

Minimal Android app that receives ACTION_SEND/ACTION_SEND_MULTIPLE and shows:
action, MIME type, flags, EXTRA_STREAM, ClipData, other extras, URI MIME type,
display name, and whether the URI can be opened for reading.

Open in Android Studio, build/install, then share the same PDF to this app from
Chrome, Samsung My Files, Google Drive, etc.

The key comparison is EXTRA_STREAM versus CLIPDATA.
