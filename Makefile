GRADLE := ./gradlew
GRADLE_SHADOWJAR := shadowJar

.PHONY: build

build:
	$(GRADLE) $(GRADLE_SHADOWJAR)

clean:
	$(GRADLE) clean

test:
	$(GRADLE) test
