from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from station import app as app_module
from station.config import StationConfig


@pytest.fixture
def client(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> TestClient:
    (tmp_path / "maia").mkdir()
    (tmp_path / "maia/maia-1500.pb.gz").write_bytes(b"")
    (tmp_path / "maia/maia-1100.pb.gz").write_bytes(b"")
    (tmp_path / "stockfish.exe").write_bytes(b"")
    config = StationConfig(
        stockfish=tmp_path / "stockfish.exe",
        lc0=tmp_path / "missing-lc0.exe",
        maia_dir=tmp_path / "maia",
        ollama_url="http://127.0.0.1:1",  # nothing listens there
        results_dir=tmp_path / "results",
    )
    app_module.app.dependency_overrides[app_module.get_config] = lambda: config
    monkeypatch.setattr(app_module, "gpu_name", lambda: "Test GPU")
    yield TestClient(app_module.app)
    app_module.app.dependency_overrides.clear()


def test_health_reports_engines_and_services(client: TestClient) -> None:
    response = client.get("/health")
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert body["gpu"] == "Test GPU"
    assert body["engines"] == {"stockfish": True, "lc0": False, "maia": ["1100", "1500"]}
    assert body["ollama"] == {"reachable": False, "models": []}


def test_ollama_models_are_listed(client: TestClient, monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setattr(app_module, "ollama_models", lambda url: ["qwen3:14b"])
    assert client.get("/health").json()["ollama"] == {"reachable": True, "models": ["qwen3:14b"]}
