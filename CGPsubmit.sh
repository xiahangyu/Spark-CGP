~/spark/spark-1.2.0-bin-hadoop2.4/bin/spark-submit \
--master spark://172.16.124.13:7077 \
--name CGP \
--class com.xhy.CGP \
--executor-memory 5G \
--total-executor-cores 8 \
~/CGP/myCGP.jar \
cgp \
~/CGP/Data/cgp.par \
~/CGP/Data/mux11.plu

