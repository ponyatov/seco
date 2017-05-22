TEX =
FIG = /tmp/seco1.pdf /tmp/seco2.pdf /tmp/hello.pdf

.PHONY: doc
doc: SECO_Developer.pdf

LATEX = pdflatex -interaction=nonstopmode -output-directory=/tmp
/tmp/SECO_Developer.pdf: SECO_Developer.tex $(TEX) $(FIG)
	$(LATEX) $< && $(LATEX) $<
	
%.pdf: /tmp/%.pdf
	mv $< $@
	
/tmp/%.pdf: fig/%.png
	convert $< $@

#docs/rse.tex: ./sigplanconf.cls
#./sigplanconf.cls:
#	wget -O $@ http://www.sigplan.org/sites/default/files/sigplanconf.cls
