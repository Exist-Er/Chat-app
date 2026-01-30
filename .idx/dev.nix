{ pkgs, ... }: {
  channel = "stable-23.11";
  packages = [
    pkgs.python311
    pkgs.openjdk17
    pkgs.python311Packages.pip
    pkgs.python311Packages.fastapi
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
    };
    previews = {
      enable = true;
      previews = {
        web = {
          # Attempt to run with the venv python first, fallback to system python if it fails
          command = ["bash" "-c" "if [ -f .venv/bin/python3 ]; then ./.venv/bin/python3 -m uvicorn app.main:app --host 0.0.0.0 --port $PORT --reload; else python3 -m uvicorn app.main:app --host 0.0.0.0 --port $PORT --reload; fi"];
          manager = "web";
          cwd = "backend";
        };
      };
    };
  };
}
