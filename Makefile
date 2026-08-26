# bom-poc source export — produces a renamed copy outside this repo.
.PHONY: export export-verify export-clean check-export-tools check-out-dir test-export-fixture

SOURCE_PACKAGE ?= org.poc.objs
CLEANUP_CONFIG ?= scripts/export/cleanup.yml
RSYNC_EXCLUDES := \
	--exclude .git/ \
	--exclude .gradle/ \
	--exclude .gradle-home/ \
	--exclude build/ \
	--exclude bin/ \
	--exclude node_modules/ \
	--exclude dist/ \
	--exclude .data/ \
	--exclude .idea/ \
	--exclude .cursor/ \
	--exclude docs/workitems/ \
	--exclude docs/.obsidian/ \
	--exclude scripts/export/ \
	--exclude Makefile

ifndef TARGET_PACKAGE
export TARGET_PACKAGE
endif

MODULE_PREFIX ?= $(lastword $(subst ., ,$(TARGET_PACKAGE)))
ROOT_PROJECT_NAME ?= $(MODULE_PREFIX)
API_VERSION ?= $(shell python3 -c 'p="$(TARGET_PACKAGE)".split("."); print(".".join(reversed(p))+"/v1")' 2>/dev/null)
OUT_DIR ?= ../bom-export-$(subst .,-,$(TARGET_PACKAGE))

check-export-tools:
	@command -v rsync >/dev/null || (echo "rsync required" && exit 1)
	@command -v python3 >/dev/null || (echo "python3 required" && exit 1)
	@python3 -c "import yaml" 2>/dev/null || (echo "python3 PyYAML required (pip install pyyaml)" && exit 1)

test-export-fixture: check-export-tools
	python3 scripts/export/test_fixture.py

check-out-dir:
	@if [ -z "$(OUT_DIR)" ]; then \
		echo "OUT_DIR is required"; \
		exit 1; \
	fi
	@python3 scripts/export/check_out_dir.py \
		--out-dir "$(OUT_DIR)" \
		--source-repo "$(CURDIR)"

export: check-export-tools check-out-dir
	@if [ -z "$(TARGET_PACKAGE)" ]; then \
		echo "TARGET_PACKAGE is required, e.g. make export TARGET_PACKAGE=com.acme.platform"; \
		exit 1; \
	fi
	@case "$(TARGET_PACKAGE)" in \
		*[!a-z0-9.]*) echo "TARGET_PACKAGE must use lowercase Java package segments"; exit 1;; \
	esac
	@python3 -c "import re; p='$(TARGET_PACKAGE)'; assert re.fullmatch(r'[a-z][a-z0-9]*(?:\\.[a-z][a-z0-9]*)+', p), p"
	@OUT_ABS="$$(python3 scripts/export/check_out_dir.py \
		--out-dir "$(OUT_DIR)" \
		--source-repo "$(CURDIR)" \
		--print)"; \
	echo "Removing existing export directory: $$OUT_ABS"; \
	rm -rf "$$OUT_ABS"; \
	mkdir -p "$$OUT_ABS"
	@echo "Exporting to $(OUT_DIR) (package $(TARGET_PACKAGE), prefix $(MODULE_PREFIX), apiVersion $(API_VERSION))"
	rsync -a $(RSYNC_EXCLUDES) ./ "$(OUT_DIR)/"
	chmod +x "$(OUT_DIR)/gradlew"
	python3 scripts/export/generate-config.py \
		--out-dir "$(OUT_DIR)" \
		--target-package "$(TARGET_PACKAGE)" \
		--module-prefix "$(MODULE_PREFIX)" \
		--api-version "$(API_VERSION)" \
		--root-project-name "$(ROOT_PROJECT_NAME)" \
		--cleanup-config "$(CLEANUP_CONFIG)"
	python3 scripts/export/dumper.py \
		--root "$(OUT_DIR)" \
		--config "$(OUT_DIR)/.dumper.yml" \
		--source-repo "$(CURDIR)"
	@echo ""
	@echo "Export complete: $(OUT_DIR)"
	@echo "Verify: make export-verify OUT_DIR=$(OUT_DIR) MODULE_PREFIX=$(MODULE_PREFIX)"
	@echo "Build:  cd $(OUT_DIR) && ./gradlew clean build"

export-clean: check-out-dir
	@OUT_ABS="$$(python3 scripts/export/check_out_dir.py \
		--out-dir "$(OUT_DIR)" \
		--source-repo "$(CURDIR)" \
		--print)"; \
	echo "Removing export directory: $$OUT_ABS"; \
	rm -rf "$$OUT_ABS"

export-verify: check-export-tools
	@if [ -z "$(OUT_DIR)" ]; then \
		echo "OUT_DIR is required"; \
		exit 1; \
	fi
	@if [ -z "$(MODULE_PREFIX)" ]; then \
		echo "MODULE_PREFIX is required (last segment of TARGET_PACKAGE)"; \
		exit 1; \
	fi
	@echo "== Grep checks =="
	@if rg 'org\.poc\.objs' "$(OUT_DIR)" --glob '**/META-INF/**' 2>/dev/null; then \
		echo "FAIL: org.poc.objs in META-INF"; exit 1; \
	else echo "OK: META-INF"; fi
	@if rg 'objs\.poc\.org/v1' "$(OUT_DIR)" --glob '!docs/workitems/**' 2>/dev/null; then \
		echo "FAIL: objs.poc.org/v1 leftover"; exit 1; \
	else echo "OK: seed apiVersion"; fi
	@if rg 'org\.poc\.objs|:objs-(core|service-ui|service-app|service|gremlin-core|gremlin-service)\b' "$(OUT_DIR)" --glob '!docs/workitems/**' 2>/dev/null; then \
		echo "FAIL: leftover POC package or :objs- gradle refs"; exit 1; \
	else echo "OK: POC identifiers"; fi
	@if rg ':sbom-service[^-]|:sbom-service$$|:asset-repository-service[^-]|:asset-repository-service$$' "$(OUT_DIR)" 2>/dev/null; then \
		echo "FAIL: unprefixed example module gradle refs"; exit 1; \
	else echo "OK: example modules"; fi
	@if rg 'docs/workitems/' "$(OUT_DIR)/docs/design" "$(OUT_DIR)/README.md" "$(OUT_DIR)/AGENTS.md" 2>/dev/null; then \
		echo "FAIL: docs/workitems links in exported docs"; exit 1; \
	else echo "OK: doc links"; fi
	@echo "== UI builds =="
	@for ui in \
		"$(MODULE_PREFIX)-service-ui" \
		"examples/sbom/$(MODULE_PREFIX)-sbom-service-ui" \
		"examples/asset-repository/$(MODULE_PREFIX)-asset-repository-service-ui"; do \
		if [ -f "$(OUT_DIR)/$$ui/package.json" ]; then \
			echo "npm ci + build in $$ui"; \
			( cd "$(OUT_DIR)/$$ui" && npm ci && npm run build ); \
		fi; \
	done
	@echo "== Gradle build =="
	cd "$(OUT_DIR)" && ./gradlew clean build
	@echo "== Seed tests =="
	cd "$(OUT_DIR)" && ./gradlew ":$(MODULE_PREFIX)-core:test" --tests '*SeedImporter*'
	@echo "export-verify passed"
