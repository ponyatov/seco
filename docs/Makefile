TEX =
FIG = fig/seco1.png fig/seco2.png fig/hello.png fig/hellobtn.png
FIG += fig/htitle.png fig/dbhello.png

.PHONY: doc
doc: SECO_Developer.pdf

LATEX = pdflatex -interaction=nonstopmode -output-directory=../tmp
SECO_Developer.pdf: SECO_Developer.tex $(TEX) $(FIG)
	$(LATEX) $< && $(LATEX) $< && mv ../tmp/$@ $@

#docs/rse.tex: ./sigplanconf.cls
#./sigplanconf.cls:
#	wget -O $@ http://www.sigplan.org/sites/default/files/sigplanconf.cls
