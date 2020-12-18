from typing import TypedDict


class PackageEntry(TypedDict):
    """
    A JSON-deserialized package record produced by the unpacker process with --output RECORDS.
    """
    # the absolute package path of the record
    path: str
    # the package contents
    package: dict
