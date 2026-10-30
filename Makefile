JAVAC = javac
JAVA = java
SRC_DIR = src/main/java
OUT_DIR = out
MAIN_CLASS = com.audy.Main

SOURCES = $(shell find $(SRC_DIR) -name "*.java")

all: run

compile:
	@echo "Compiling the Audy project..."
	@mkdir -p $(OUT_DIR)
	$(JAVAC) -d $(OUT_DIR) $(SOURCES)
	@echo "Compilation completed successfully!"

run: compile
	@echo "Launching the application..."
	$(JAVA) -cp $(OUT_DIR) $(MAIN_CLASS)

clean:
	@echo "Removing the $(OUT_DIR)/ directory..."
	rm -rf $(OUT_DIR)
	@echo "Cleanup completed."

.PHONY: all compile run clean