(defproject com.github.flexiana/infix "1.0-rc1"
  :description "Readable Math and Dataflow Syntax for Clojure with infix expressions, OOP interop, and threading macros"
  :url "https://github.com/flexiana/infix"
  :license {:name "Eclipse Public License 2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  
  :dependencies [[org.clojure/clojure "1.11.1"]]
  
  :source-paths ["src"]
  :test-paths ["test"]
  
  :profiles {:dev {:dependencies [[org.clojure/test.check "1.1.1"]]}
             :test {:dependencies [[org.clojure/test.check "1.1.1"]]}}
  
  :aliases {"test-all" ["test"]}
  
  :scm {:name "git"
        :url "https://github.com/flexiana/infix"}
  
  :deploy-repositories [["clojars" {:url "https://repo.clojars.org/"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases false}]])