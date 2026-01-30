{ pkgs, ... }: {
  channel = "stable-23.11";
  packages = [
    pkgs.python311
    pkgs.openjdk17
    pkgs.python311Packages.pip
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
          # Use bash -c to ensure the virtual environment is sourced correctly
          command = ["bash" "-c" "source .venv/bin/activate && python3 -m uvicorn app.main:app --host 0.0.0.0 --port $PORT --reload"];
          manager = "web";
          cwd = "backend";
        };
      };
    };
  };
}
