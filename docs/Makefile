TEX =
FIG = /tmp/seco.pdf

.PHONY: doc
doc: SECO_Developer.pdf

LATEX = pdflatex -interaction=nonstopmode -output-directory=/tmp
/tmp/%.pdf: %.tex $(TEX) $(FIG)
	$(LATEX) $<
	
%.pdf: /tmp/%.pdf
	mv $< $@
	
/tmp/%.pdf: fig/%.png
	echo $< $@

#docs/rse.tex: ./sigplanconf.cls
#./sigplanconf.cls:
#	wget -O $@ http://www.sigplan.org/sites/default/files/sigplanconf.cls
