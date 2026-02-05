{
  inputs = {
    nixpkgs.url = "github:cachix/devenv-nixpkgs/rolling";
    systems.url = "github:nix-systems/default";
    devenv = {
      url = "github:cachix/devenv";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    tad-better-behavior.url = "git+https://codeberg.org/tad-lispy/tad-better-behavior.git";
    clj-nix.url = "github:jlesquembre/clj-nix";
  };

  nixConfig = {
    extra-trusted-public-keys = "devenv.cachix.org-1:w1cLUi8dv3hnoSPGAuibQv+f9TZLr6cv/Hm9XgU50cw=";
    extra-substituters = "https://devenv.cachix.org";
  };

  outputs = { self, nixpkgs, devenv, systems, clj-nix, tad-better-behavior, ... } @ inputs:
    let
      forEachSystem = nixpkgs.lib.genAttrs (import systems);
    in
      {
        devShells = forEachSystem
          (system:
            let
              pkgs = nixpkgs.legacyPackages.${system};
              clj-nix-pkgs = inputs.clj-nix.packages.${system};
            in
              {
                default = devenv.lib.mkShell {
                  inherit inputs pkgs;
                  modules = [
                    {
                      # https://devenv.sh/reference/options/
                      languages.clojure.enable = true;
                      languages.java = {
                        enable  = true;
                        jdk.package = pkgs.temurin-bin;
                      };
                      packages = with pkgs; [
                        httpie
                        clj-kondo
                        babashka
                        cljstyle
                        clojure-lsp
                        geckodriver
                        tad-better-behavior.packages.${system}.default
                        clj-nix-pkgs.deps-lock
                        miniserve
                      ];
                    }
                  ];
                };
              });
        packages = forEachSystem
          (system:
            let
              pkgs = nixpkgs.legacyPackages.${system};
              clj-pkgs = clj-nix.packages.${system};
            in
              {
                default = clj-nix.lib.mkCljApp {
                  inherit pkgs;
                  modules = [
                    # Option list:
                    # https://jlesquembre.github.io/clj-nix/options/
                    {
                      projectSrc = ./.;
                      name = "form-to-mail";
                      main-ns = "app.core";

                      # nativeImage.enable = true;

                      # customJdk.enable = true;
                    }
                  ];
                };
                uberjar = clj-pkgs.mkCljBin {
                  projectSrc = ./.;
                  name = "form-to-mail";
                  main-ns = "app.core";
                  buildCommand = "clj -T:build uber";

                  # nativeImage.enable = true;

                  # customJdk.enable = true;
                };
              });
      };
}
