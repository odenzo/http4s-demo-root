# File Encoding Summary

IWe determine the character set by the BOM, but if we leave the BOM in it confuses XML Parser.
BOM is expected for UTF-16, but not if we parse with the BOM and UTF-16BE then XML Parser not expectuing BOM
Is we keep as 