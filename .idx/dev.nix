{ pkgs, ... }: {
  channel = "stable-23.11";
  packages = [
    pkgs.python311
    pkgs.openjdk17
    pkgs.python311Packages.pip
    pkgs.python311Packages.uvicorn
  ];
  idx = {
    extensions = [
      "ms-python.python"
      "vscjava.vscode-java-pack"
      "kotlin"
    ];
    workspace = {
      onCreate = {
        setup-venv = "python3 -m venv backend/.venv && ./backend/.venv/bin/pip install -r backend/requirements.txt";
      };
      onStart = {
        # Optional: ensure deps are synced on start
        sync-deps = "./backend/.venv/bin/pip install -r backend/requirements.txt";
      };
    };
    previews = {
      enable = true;
      previews = {
        web = {
          command = ["./.venv/bin/uvicorn" "app.main:app" "--host" "0.0.0.0" "--port" "$PORT" "--reload"];
          manager = "web";
          cwd = "backend";
        };
      };
    };
  };
}
