{
  description = "GitDown";
  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixpkgs-unstable";
  inputs.flake-utils.url = "github:numtide/flake-utils";

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system: let
      pkgs = nixpkgs.legacyPackages.${system};
    in {

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
          ];

          LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath [
            pkgs.mesa
            pkgs.libglvnd
            pkgs.xorg.libX11
            pkgs.xorg.libXrandr
            pkgs.xorg.libXcursor
            pkgs.xorg.libXi
            pkgs.xorg.libXext
            pkgs.fontconfig
          ];

      };

    });
}
