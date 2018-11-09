package com.xhy

import com.xhy.MyPregel
import com.xhy.CGP_Para
import com.xhy.Chromosome
import com.xhy.Chromosomes

import org.apache.spark._
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

import java.io._
import java.util.Random

class CGP_Function extends Serializable{
   @transient
   val conf = new SparkConf()
   
   @transient
   val sc = new SparkContext(conf)
   
   var cgp_Para:CGP_Para = null
   
  var random = new Random()
  random.setSeed(System.currentTimeMillis())
    
  def this( cgp_Parameters:CGP_Para){
    this()
    cgp_Para=cgp_Parameters
  }
      
  def run_EA():Unit = {
    println("--------------------start run_EA()------\n")
      var stat:String=""
      var best_gen, run:Int = 0
      var worst_of_best_fit = cgp_Para.num_bits + cgp_Para.num_nodes
      var best_of_best_fit = 0 
      var fitness_final = 0
      var fitnesses = new Array[Int](cgp_Para.num_runs_total)
      var start, end:Long =0
      var runtimes = new Array[Double](cgp_Para.num_runs_total)
      var suc_rate : Double =0
      
      //EA run
      val writer = new PrintWriter(new File("//home//pc//CGP//Output//Results.txt"),"UTF-8")
      for( run <- 0 until cgp_Para.num_runs_total){
            writer.write("\nRun:" + run +  "		full match:" + cgp_Para.num_bits + "\n")
            
              start = System.currentTimeMillis()
              var chromosome_final = EA()
              end = System.currentTimeMillis()
              runtimes(run) = (end - start) 
            
            if(chromosome_final==null){
              println("Failed!")
            }else{
              fitness_final = chromosome_final.fitness
              if( fitness_final < worst_of_best_fit ){
                  worst_of_best_fit = fitness_final
              }
              if( fitness_final > best_of_best_fit){
                  best_of_best_fit=fitness_final
              }
              fitnesses(run) = fitness_final
              
              if( fitness_final == cgp_Para.num_bits){
                  writer.write("Succeed ! Best_fit:" + fitness_final + "\n")
                  writer.write("Chromosome: ")
                  fprint_Chromo(chromosome_final , writer)
                  suc_rate = suc_rate+1
              }else {
                  writer.write("Failed ! Best_fit:" + fitness_final + "\n")
              }
              writer.write("process_time:" + runtimes(run) +"ms\n")
              //writer.write( "num_gene:" + chromosome_final.num +" up:"+chromosome_final.up +"\n")
            }
      }
      writer.write("\nSucceed rate : " + suc_rate + "/" + cgp_Para.num_runs_total + "   Avg_runtime:" + (runtimes.sum/cgp_Para.num_runs_total).toInt + "ms Avg_fitness:" + fitnesses.sum/cgp_Para.num_runs_total + "\n")
      writer.close()
  }
      
  def EA() : Chromosome = {
        var graph  = get_CGP_graph()
        if(graph==null){
          println("Get null graph!")
          return null
        }
        
        println("initial graph:")
        var vertexs = graph.vertices.collect()
        vertexs.foreach(vertex => {
            print(vertex._1 + ":" + vertex._2.bestChromo_fitness + " ")
        })
        println()
        
        //Mind: allocate a new space to store msg is neccessary
      def vertexProgram( id: VertexId, attr:Chromosomes, msg:Chromosome) : Chromosomes = {
         var newAttr = new Chromosomes() 
         chromos_initial(newAttr)
         chromos_clone(newAttr, attr)
         
         if( msg.fitness > newAttr.bestChromo_fitness) {
              chromos_updata(newAttr, msg)
          }
          if(attr.bestChromo_fitness < cgp_Para.num_bits){
              chromos_mutate( newAttr, 1000) //mutate里面包含了choose
          }
          newAttr
      }
      def sendMessage( triplet: EdgeContext[Chromosomes, Int, Chromosome] ) : Unit = {
              if(triplet.dstAttr.bestChromo_fitness!=cgp_Para.num_bits){
                  if( triplet.srcAttr.bestChromo_fitness>=triplet.dstAttr.bestChromo_fitness ){
                      triplet.sendToDst ( chromos_getBestChromo( triplet.srcAttr ) ) 
                  }
              }
       }
      def messageCombiner( a:Chromosome, b:Chromosome) : Chromosome =  {
              if( a.fitness < b.fitness) {
                b
              }else{
                a
              }
     }
      
      var initialMsg = new Chromosome()
      chromo_initial(initialMsg)
      graph = MyPregel( graph, initialMsg, Int.MaxValue) ( vertexProgram, sendMessage, messageCombiner)
      
      var result:Chromosome = new Chromosome()
      chromo_initial(result)
      
      var verts:Array[(VertexId, Chromosomes)]=null
      verts = graph.vertices.collect()
      for( i <- 0 until cgp_Para.num_points){
          if( verts(i)._2.bestChromo_fitness==cgp_Para.num_bits ){
              chromo_clone( result, chromos_getBestChromo(verts(i)._2) )
              return result
          }
      }
     chromo_clone( result, chromos_getBestChromo(verts(0)._2) )
     result
  }
      
  def get_CGP_graph() : Graph[Chromosomes, Int] = {
        if(cgp_Para.num_points == 0){
          println("Num_Points 为0\n")
          return null
        }
        
        var ArrVertexs = new Array[(VertexId, Chromosomes)] (cgp_Para.num_points)
        var chromos:Chromosomes=null
        for(i <- 0 until cgp_Para.num_points){
          chromos = new Chromosomes()
          chromos_initial(chromos)
          ArrVertexs(i) = ( i, chromos )
        }
        val RDDVertexs : RDD[(VertexId,Chromosomes)] = sc.parallelize( ArrVertexs)
        
         
        var ArrEdges = new Array[Edge[Int]] ( (cgp_Para.num_points-1)*2 ) //边属性在本程序中并无意义
        for( i <- 0 until cgp_Para.num_points -1 ){
          ArrEdges(i) = new Edge( i+1L, 0L, 0) //构造节点0的入边
        }
        for( i <- cgp_Para.num_points -1 until (cgp_Para.num_points-1)*2){
          ArrEdges(i) = new Edge( 0, i - cgp_Para.num_points + 2 , 0)  //构造节点0的出边
        }
        var RDDEdges : RDD[Edge[Int]] = sc.parallelize(ArrEdges)
        
        /*
         var ArrEdges = new Array[Edge[Int]] ( cgp_Para.num_points ) //边属性在本程序中并无意义
        for( i <- 0 until cgp_Para.num_points -1 ){
          ArrEdges(i) = new Edge( i+0L, i+1L, 0) //构造节点0的入边
        }
        ArrEdges(cgp_Para.num_points -1) = new Edge(cgp_Para.num_points -1L, 0L, 0)
        var RDDEdges : RDD[Edge[Int]] = sc.parallelize(ArrEdges)
        */
        
        Graph( RDDVertexs,RDDEdges)
  }
  
  /*------------------------Ｃhromo_function*/
  def chromo_initial( chromo:Chromosome):Unit={
     var node_i = 0
      var pre_column = 0
      
      chromo.fitness=0
      chromo.outputs = new Array[Int] (cgp_Para.num_outputs)
      chromo.gene = new Array[Array[Int]] (cgp_Para.num_nodes)
      chromo.gene = chromo.gene.map( node => new Array[Int](cgp_Para.num_genes_per_node))
      
      chromo.gene.foreach{ node=>{
            pre_column = node_i/cgp_Para.num_rows
            for( i <- 0 until cgp_Para.num_genes_per_node-1){
              node(i) = get_connection_gene( pre_column)
            }
            node(cgp_Para.num_genes_per_node-1) = get_function_gene( )
            node_i = node_i + 1
          }
       }
      chromo.outputs = chromo.outputs.map( gene => get_output_gene() )
      
      chromo_get_fitness(chromo)
  }
  
  def chromo_clone( a:Chromosome, b:Chromosome):Unit = {
    //a clone b
      for( i <- 0 until cgp_Para.num_nodes ){
            for( j <- 0 until cgp_Para.num_genes_per_node){
                a.gene(i)(j) = b.gene(i)(j)
            }
        }
       
        for( i <- 0 until cgp_Para.num_outputs){
            a.outputs(i) = b.outputs(i)
        }
        a.fitness = b.fitness
  }
  
  def chromo_mutate(chromo:Chromosome) : Unit = {
        var num_mutant = (cgp_Para.num_genes * cgp_Para.per_cent_mutate/100.0).toInt
        var node_i, gene_i = 0 //变异的节点和基因位置
        var new_gene , old_gene = 0
        
        for( j <- 0 until num_mutant){
            node_i = random.nextInt( Int.MaxValue )%(cgp_Para.num_nodes+cgp_Para.num_outputs)
            if( node_i < cgp_Para.num_nodes){
             //计算节点上的基因变异
                gene_i = random.nextInt( Int.MaxValue ) % cgp_Para.num_genes_per_node
                var pre_column = node_i/cgp_Para.num_rows
                old_gene = chromo.gene(node_i)(gene_i)
                
                if( gene_i != cgp_Para.num_genes_per_node - 1 ) { //连接基因变异
                    chromo.gene (node_i)(gene_i) = get_connection_gene( pre_column)
                }else{//功能基因变异
                    chromo.gene (node_i)(gene_i) = get_function_gene()
                }
            }else { 
              //输出节点上的基因变异
                node_i = node_i - cgp_Para.num_nodes 
                chromo.gene (node_i)(gene_i) = get_output_gene()
            }
        }
        chromo.fitness = 0
      }
  
    def chromo_get_fitness( chromo:Chromosome):Int={
      if( chromo.fitness !=  0 ) {
            return chromo.fitness
        }
        var in = new Array[Long](cgp_Para.num_genes_per_node-1)
        var out = new Array[Long](cgp_Para.end_count + cgp_Para.num_outputs) 
        
        var out_i, function_type = 0
        for( test <- 0 until cgp_Para.num_products){
              for( i <- 0 until cgp_Para.num_inputs){
                  out(i) = cgp_Para.plu_inputs(test)(i)
              }
              
              out_i =   cgp_Para.num_inputs
              for( k <- 0 until cgp_Para.num_nodes){
                  for( i <- 0 until cgp_Para.num_genes_per_node -1 ){
                      in(i) = out( chromo.gene(k)(i) )
                  }
                  function_type =   chromo.gene(k)(cgp_Para.num_genes_per_node - 1) 
                  out( out_i ) = node_type( in, function_type)
                  out_i = out_i + 1
              }
              
              chromo.outputs.foreach { output_node => {
                      out(out_i) = out( output_node)
                      out_i = out_i+1
                  }
              }

              for( i <- 0 until cgp_Para.num_outputs){
                  chromo.fitness = chromo.fitness + invhamming( out( cgp_Para.end_count + i), cgp_Para.plu_outputs(test)(i) )
              }
        }
        chromo.fitness
  }
  
  def get_connection_gene(pre_column :Int) : Int = {
        ( random.nextInt(Int.MaxValue) % ( cgp_Para.num_inputs + pre_column * cgp_Para.num_rows))
    }
  
  def get_function_gene() :Int = {
       cgp_Para.allowed_functions ( random.nextInt(Int.MaxValue)  % cgp_Para.num_functions)
    }
  
    def get_output_gene() : Int = {
      ( random.nextInt(Int.MaxValue) % ( cgp_Para.num_inputs + cgp_Para.num_nodes)  )
    }
    
    def node_type( in:Array[Long], function_type:Int) : Long = {
          var result = 0L ;
          if( function_type == 6){
              result = ( in(0) & in(1) )
          }else if(function_type == 9){
              result = ( ~in(0) & ~in(1) )
          }else if(function_type==12){
              result = ( in(0) | in(1) )
          }else if(function_type==15){
              result =  ( ~in(0) | ~in(1) )
          }
          result
    }
  
    def invhamming ( a:Long, b:Long) : Int = {
      var result:Int = 0
      var temp : Long = (~a)^b
      
      for( i <- 0 until cgp_Para.bit_width) {
          result = result + cgp_Para.getbit(temp,i)
      }
      result
    }
    
  def fprint_Chromo( chromo:Chromosome, writer:PrintWriter) :Unit = {
        var node_i = cgp_Para.num_inputs
        writer.write( "{" )
        if(chromo.gene!=null){
          chromo.gene.foreach ( node => {
                  writer.write( " (" )
                  for( i <- 0 until cgp_Para.num_genes_per_node - 1){
                    writer.write( node(i) + ",")
                  }
                  writer.write( node(cgp_Para.num_genes_per_node - 1) + "):" + node_i )
                  node_i = node_i +1
              }
          )
          chromo.outputs.foreach( output => {
                writer.write( " " + output)
            })
          writer.write( " }\n" )
        }else{
          println("Best_chromo gene is null.")
        }
  }
  
  /*----------------------------Chromosomes_function-------------------------*/
  def chromos_initial( chromos:Chromosomes):Unit={
    chromos.indivisuals = new Array[Chromosome]( cgp_Para.population_size)
    
    for( i <- 0 until cgp_Para.population_size) {
        chromos.indivisuals(i) = new Chromosome() 
        chromo_initial(chromos.indivisuals(i))
    }
    
    chromos_choose(chromos)
  }
  
  def chromos_clone( a:Chromosomes, b:Chromosomes):Unit={
     for( i <- 0 until cgp_Para.population_size){
         chromo_clone( a.indivisuals(i), b.indivisuals(i))
     }
     a.bestChromo_fitness=b.bestChromo_fitness
     a.bestChromo_index=b.bestChromo_index
  }
  
  def chromos_choose( chromos:Chromosomes) :Unit = {
      chromos.bestChromo_fitness =0
      
      for( i <- 0 until cgp_Para.population_size ) {
          if( chromos.indivisuals(i).fitness >= chromos.bestChromo_fitness ){
              chromos.bestChromo_fitness =  chromos.indivisuals(i).fitness
              chromos.bestChromo_index = i
          }
      }
      
      chromo_clone(chromos.indivisuals(cgp_Para.population_size -1), chromos.indivisuals( chromos.bestChromo_index ))
      chromos.bestChromo_index = cgp_Para.population_size  - 1
  }
  
  def  chromos_updata( chromos:Chromosomes,  msgChromosome:Chromosome) :Unit = {
        chromo_clone( chromos_getBestChromo(chromos), msgChromosome )
        chromos.bestChromo_fitness = msgChromosome.fitness
  }
  
  def chromos_mutate( chromos:Chromosomes,num:Int) : Unit = {
    chromos.prebestChromo_fitness = chromos.bestChromo_fitness
      for( i <- 0 until num){
          for( j <- 0 until cgp_Para.population_size-1){ //默认将父母然染色体(最优的染色体)放在数组最后
              chromo_clone( chromos.indivisuals(j), chromos.indivisuals( chromos.bestChromo_index) )
              chromo_mutate( chromos.indivisuals(j) )
              chromo_get_fitness( chromos.indivisuals(j) )
          }
          chromos_choose(chromos)
      }
  }
  
  def chromos_getBestChromo(chromos:Chromosomes):Chromosome = {
     chromos.indivisuals(chromos.bestChromo_index)
  }
}