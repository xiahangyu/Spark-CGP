package com.xhy

import com.xhy.Chromosome 
import com.xhy.Chromosomes

import scala.reflect.ClassTag
//import org.apache.spark.Logging

import org.apache.spark._
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD

object MyPregel extends Serializable{
  
    def apply//[Chromosomes: ClassTag, ED: ClassTag, Chromosome: ClassTag] 
    (graph: Graph[Chromosomes, Int],
    initialMsg: Chromosome,
    maxIterations: Int = Int.MaxValue,
    activeDirection: EdgeDirection = EdgeDirection.Either)
    (vprog: (VertexId, Chromosomes, Chromosome) => Chromosomes,
    sendMsg: EdgeContext[Chromosomes, Int, Chromosome] => Unit,
    mergeMsg:(Chromosome, Chromosome) => Chromosome,
    tripletFields: TripletFields = TripletFields.All)
    : Graph[Chromosomes, Int] = {
              println("initial message:" + initialMsg.fitness)
              var g = graph.mapVertices(  (vid, vdata) => vprog(vid, vdata, initialMsg)).cache()
              
              var vertices = g.vertices.collect()
                  println("graph first::")
                  vertices.foreach( vertex =>{
                    print( vertex._1+ ":" + vertex._2.bestChromo_fitness + " ")
                  })
                  println()
                  
              var messages = g.aggregateMessages(sendMsg, mergeMsg)
                 
              /*
              println("Initial message:")
              var ms = messages.collect()
              ms.foreach( me => {
                println(me._1+ "<-" + me._2.fitness)
              })*/
              
              var activeMessages = messages.count()
              var prevG: Graph[Chromosomes, Int] = null
              var i = 0
              while (activeMessages > 0 && i < maxIterations) {
                  prevG = g
                  g = g.joinVertices(messages)(vprog).cache()
                  
                  
                   vertices = g.vertices.collect()
                  println("graph after::")
                  vertices.foreach( vertex =>{
                    print( vertex._1+ ":" + vertex._2.bestChromo_fitness + " ")
                  })
                  println()
                  println("activeMessages::" + activeMessages + " " + i+":/"+maxIterations)
                  
                  val oldMessages = messages
                  messages = g.aggregateMessages( sendMsg, mergeMsg).cache()
                  /*
                  println("message:")
                  ms = messages.collect()
                  ms.foreach( mg =>{
                    println( mg._1 + "<-" + mg._2.fitness )
                  })*/
                  activeMessages = messages.count()
                   
                  oldMessages.unpersist()
                  prevG.unpersistVertices()
                  prevG.edges.unpersist()
    
                  i += 1
              }
              g
        }
}