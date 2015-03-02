build:
	mvn clean install -Dmaven.test.skip=true

publish:
	mvn clean deploy -P release -Dmaven.test.skip=true -pl \!io.mappum:altcoinj-tools,\!io.mappum:altcoinj-examples
