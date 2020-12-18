from src.utils import desanitize
from src.utils.types import SemVer


def helper(version: str, result: str):
    return str(SemVer.from_str(desanitize(version))) == result


def test_update_version_parser_0():
    assert helper("SGVhcnQgb2YgRGVpbW9zOiBIb3RmaXggMjkuMC42", "29.0.6")


def test_update_version_parser_1():
    assert helper("SGVhcnQgb2YgRGVpbW9zOiBVcGRhdGUgMjk=", "29.0.0")


def test_update_version_parser_2():
    assert helper("VGhlIFN0ZWVsIFBhdGg6IEluYXJvcyBQcmltZSAyOC4yLjAgKyAyOC4yLjAuMQ==", "28.2.0.1")


def test_update_version_parser_3():
    assert helper("VXBkYXRlIDI4OiBUaGUgRGVhZGxvY2sgUHJvdG9jb2w=", "28.0.0")
