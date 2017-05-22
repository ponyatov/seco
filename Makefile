CWD = $(CURDIR)

all: doc

.PHONY: doc
doc:
	cd docs ; $(MAKE)
