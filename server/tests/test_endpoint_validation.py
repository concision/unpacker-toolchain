from fastapi.testclient import TestClient

from src.main import app


client = TestClient(app)


def test_versions_validation_0():
    response = client.get("/versions?from=[ph]&to=[ph]&count=20")
    assert response.status_code == 422


def test_version_packages_validation_0():
    response = client.get("/versions/[ph]/packages?full=True")
    assert response.status_code == 422


def test_version_packages_validation_1():
    response = client.get("/versions/[ph]/packages?full=True&patterns=/**/")
    assert response.status_code == 200


def test_packages_validation_0():
    response = client.get("/packages?package=[ph]&from=[ph]&to=[ph]&count=20")
    assert response.status_code == 422
