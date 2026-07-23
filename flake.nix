{
  description = "GitDown";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    flake-parts.url = "github:hercules-ci/flake-parts";
    spindrift.url = "github:jordansmall/spindrift";
  };

  outputs = inputs@{ self, flake-parts, nixpkgs, spindrift, flake-utils }:
    flake-parts.lib.mkFlake { inherit inputs; } {
      systems = [ "x86_64-linux" "aarch64-linux" "aarch64-darwin" ];
      imports = [ spindrift.flakeModules.default ];
      perSystem = { config, pkgs, ... }: {
        spindrift = {
          packages = p: [ p.gnumake p.go ];
          prompt = builtins.readFile ./prompts/issue-prompt.md;
          settings = {
            repository = {
              repoSlug = "codymikol/git-down";
              gitUserName = "bot";
              gitUserEmail = "hi@codymikol.com";
            };
            branches = {
              mergeMode = "immediate";
            };
            concurrency = { maxParallel = 1; };
          };
        }; 
        devShells.default = pkgs.mkShell {

            _JAVA_OPTIONS = "-Dswing.useSystemFileChooser=true";
          
            packages = [
              pkgs.git
              pkgs.temurin-bin-21
              pkgs.mesa
              pkgs.libGL
              pkgs.libGLU
              pkgs.xorg.libXext
              pkgs.fontconfig
              pkgs.xorg.libX11
              pkgs.xorg.libXrandr
              pkgs.xorg.libXcursor
              pkgs.xorg.libXi
              pkgs.xorg.libXext
              pkgs.fontconfig
              config.packages.spindrift
            ];

            LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath [
              pkgs.mesa
              pkgs.libGL
              pkgs.libglvnd
              pkgs.stdenv.cc.cc.lib
              pkgs.xorg.libX11
              pkgs.xorg.libXrandr
              pkgs.xorg.libXcursor
              pkgs.xorg.libXi
              pkgs.xorg.libXext
              pkgs.fontconfig
            ];

        };
      };
    };
}
