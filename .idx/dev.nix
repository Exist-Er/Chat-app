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
    previews = {
      enable = true;
      previews = {
        web = {
          command = ["python3" "-m" "uvicorn" "app.main:app" "--host" "0.0.0.0" "--port" "$PORT" "--reload"];
          manager = "web";
          cwd = "backend";
        };
        # Android preview is handled natively by IDX when it detects an Android project
      };
    };
    workspace = {
      onCreate = {
        pip-install = "pip install -r backend/requirements.txt";
      };
    };
  };
}
