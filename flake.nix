{
  description = "GitDown";
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
  inputs.flake-utils.url = "github:numtide/flake-utils";

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system: let
      pkgs = nixpkgs.legacyPackages.${system};
    in {

      packages.default = pkgs.stdenv.mkDerivation( finalAttrs: {
        
        pname = "git-down";
        version = "0.1.0";
        src = ./.;

        nativeBuildInputs = [ pkgs.gradle ];

        mtimCache = pkgs.gradle.fetchDeps {
          inherit (finalAttrs) pname;
          data = ./deps.json;
        };

        # this is required for using mitm-cache on Darwin
        __darwinAllowLocalNetworking = true;

        buildInputs = [ pkgs.jdk11 pkgs.mesa ];

      }); 

      devShells.default = pkgs.mkShell {
        
        packages = [
            pkgs.git
            pkgs.temurin-bin-17
            pkgs.mesa
            pkgs.libGL
            pkgs.libGLU
            pkgs.xorg.libX11
            pkgs.xorg.libXext
            pkgs.fontconfig
        ];

        shellHook = ''
          echo "Setting up OpenGL environment...";
          LD_LIBRARY_PATH="${pkgs.libGL}/lib/:${pkgs.stdenv.cc.cc.lib}/lib/";
        '';

      };

      meta.sourceProvenance = with pkgs.lib.sourceTypes; [
        fromSource
        binaryBytecode # mitm cache
      ];
    });
}
