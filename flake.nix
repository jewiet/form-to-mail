{
  inputs = {
    nixpkgs.url = "github:cachix/devenv-nixpkgs/rolling";
    systems.url = "github:nix-systems/default";
    devenv = {
      url = "github:cachix/devenv";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    clj-nix.url = "github:jlesquembre/clj-nix";
  };

  nixConfig = {
    extra-trusted-public-keys = "devenv.cachix.org-1:w1cLUi8dv3hnoSPGAuibQv+f9TZLr6cv/Hm9XgU50cw=";
    extra-substituters = "https://devenv.cachix.org";
  };

  outputs = { self, nixpkgs, devenv, systems, clj-nix, ... } @ inputs:
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
                        clj-nix-pkgs.deps-lock
                        miniserve
                        (gauge.withPlugins (_: [
                          gaugePlugins.java
                          gaugePlugins.html-report
                          gaugePlugins.screenshot
                        ]))
                      ];
                    }
                  ];
                };
              });
        packages = forEachSystem
          (system:
            let
              pkgs = nixpkgs.legacyPackages.${system};
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
              });
      };
}
