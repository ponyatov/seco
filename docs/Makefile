TEX = SyntaxParser.tex Eclipse.tex gVim.tex HyperGraphDB.tex
TEX += Intro2Seco.tex UsingSeco.tex FreshTaste.tex
FIG = fig/seco1.png fig/seco2.png fig/hello.png fig/hellobtn.png
FIG += fig/htitle.png fig/dbhello.png fig/antlrtree.png
SRC = src/vim.rc src/script.g4 src/secoParser.mk src/newdb.seco
SRC += src/HGCustomClass.java src/HGBook.beanshell src/interprolog.seco
TEX += HackingSeco.tex HackingHGDB.tex
SRC += src/hgdbclone.rc src/hgdbsrc.rc
TEX += ErgoFlora.tex PrologIntro.tex ExternalLibraries.tex 
SRC += src/linflora.rc src/winflora.rc src/Hello.flr

.PHONY: doc
doc: SECO_Developer.pdf

LATEX = pdflatex -interaction=nonstopmode -output-directory=../tmp
SECO_Developer.pdf: SECO_Developer.tex $(TEX) $(FIG) $(SRC)
	$(LATEX) $< && $(LATEX) $<
# && cp ../tmp/$@ $@

#docs/rse.tex: ./sigplanconf.cls
#./sigplanconf.cls:
#	wget -O $@ http://www.sigplan.org/sites/default/files/sigplanconf.cls
