package com.xhy

import com.xhy.CGP_Para
import com.xhy.CGP_Function
import com.xhy.Chromosome
import com.xhy.Chromosomes

import scala.io.Source
import java.io._

object  CGP {
  val MAX_NUM_LETTERS:Int = 100
      
   var cgp_Para = new CGP_Para
  
   /*
   def print_Chromo( chromo:Chromosome) :Unit = {
        var node_i = cgp_Para.num_inputs 
        print( "{" )
        chromo.gene.foreach ( node => {
                print( " (" )
                for( i <- 0 until cgp_Para.num_genes_per_node - 1)
                  print( node(i) + ",")
               print( node(cgp_Para.num_genes_per_node - 1) + "):" + node_i )
               node_i = node_i + 1 
            }
        )
        chromo.outputs.foreach( output => print( " " + output))
        print( " }\n" )
  }*/
  
  def main(args: Array[String]) {
    var parfile:String=""
    var plufile :String="";
    
    var argc:Int = args.length;
    if( validate_command_line(argc, args) ) {
      parfile=args(1)
      plufile=args(2)
    }else {
      return
    }
      
    get_parameters(parfile, plufile)
    write_cgp_info( args(0), plufile)
    
    var cgp_Function = new CGP_Function( cgp_Para)
    cgp_Function.run_EA()
    
   /*
    var c = new Chromosomes(cgp_Para)
    for( i <- 0 until 1){
       println( c.bestChromo_fitness + " " +c.bestChromo_index )
       c.mutate(100)
       
       if(c.bestChromo_fitness==16){
         print_Chromo( c.getBestChromo())
         return
       }
    }
    */
    
    /*
    var graph = cgp_Function.get_CGP_graph()
    var edges = graph.triplets.collect()
    edges.foreach { edge => {
        var b = (edge.srcAttr.bestChromo_fitness ==  (cgp_Para.num_bits))
        println( edge.srcId + "->" + edge.dstId + " srcAttr:" + edge.srcAttr.bestChromo_fitness + " " + b+" " + edge.srcAttr.bestChromo_index + " max:" + cgp_Para.num_bits)
      }
    }
    */
    
    /*
    var cs = new Chromosomes(cgp_Para)
    for( i <- 0 until 50 ){
        println("i:" + i  +" " + cs.bestChromo_fitness)
        cs.mutate(10)
    }*/
    
    println("")
    println("*********    CGP COMPLETED      *********")
  }
  
  def validate_command_line( argc:Int, argv:Array[String]) : Boolean = {
    println("")
	  println("*********    WELCOME TO CARTESIAN GENETIC PROGRAMMING      *********")
    println("********* Validating command line arguments to cgp program *********")
    
    if (argc!=3)
  	{
  		println("INCORRECT NUMBER OF ARGUMENTS")
  		println("Type cgp <file.par> <file.plu> then return")
  		return false
  	}
    
    if ( argv(1).length > (MAX_NUM_LETTERS-1))
  	{
     	println("filename for parameter file is too long")
  		println("It should be less than" + MAX_NUM_LETTERS + "characters")
  		return false
  	}
    
    if ( argv(2).length > (MAX_NUM_LETTERS-1) )
  	{
     		println("filename for plu file is too long")
    		println("It should be less than" + MAX_NUM_LETTERS + "characters")
    		return false
  	}
    true
  }

  def get_parameters( parfile:String,  plufile:String) : Unit = {
    var file = Source.fromFile( parfile )
    var lines = file.getLines().flatMap( line =>  line.split(" +")).toArray
    cgp_Para.num_points = lines(0).toInt
    cgp_Para.population_size = lines(2).toInt
    cgp_Para.per_cent_mutate = lines(4).toDouble
    cgp_Para.num_generations = lines(6).toInt
    cgp_Para. num_runs_total = lines(8).toInt
    cgp_Para.num_rows = lines(10).toInt
    cgp_Para.num_cols = lines(12).toInt
    cgp_Para.levels_back  = lines(14).toInt
    cgp_Para.mu = lines(16).toInt
    
    // assigned global constants
  	 cgp_Para.num_functions=0;
  	//规定每个节点的表示维数
  	 cgp_Para.num_genes_per_node=3;
  	for( i <-  0 until cgp_Para.MAX_NUM_FUNCTIONS ){
  	  cgp_Para.number(i) = lines(18+i*2).toInt
  	  cgp_Para.node_types(i) = lines(18 + i*2 +1)
  	  
  	  if( cgp_Para.number(i) != 0  ){
  	    cgp_Para.allowed_functions( cgp_Para.num_functions ) = i
  	    cgp_Para.num_functions = cgp_Para.num_functions + 1
  	    
  	    if (i>15)
				  cgp_Para.num_genes_per_node=4;//后面四个函数为MUX类型
  	  }
  	}
  	
  	read_plu(plufile);//返回ni，no,num_nodes....output[];
  	
    if ( cgp_Para.population_size > cgp_Para.MAX_NUM_CHROMOSOMES)
  	{
  		printf("Too large a population size (<= %d)\n", cgp_Para.MAX_NUM_CHROMOSOMES);
  		return
  	}
  
  	if ( cgp_Para.num_genes > cgp_Para.MAX_NUM_GENES)
  	{
  		printf("Too many genes selected (<= %d)\n", cgp_Para.MAX_NUM_GENES);
  		return
  	}
  
  	if (cgp_Para.num_runs_total < 1)
  	{
  		println("Number of runs of EA must be at least 1");
  		return
  	}
  	else if (cgp_Para.num_runs_total > cgp_Para.MAX_NUM_RUNS)
  	{
  		printf("Number of runs of EA must be less than %d\n", cgp_Para.MAX_NUM_RUNS);
  		return
  	}
  
  	if (cgp_Para.num_genes < 10)
  	{
  		println("Number of genes/bits must be at least 10");
  		return
  	}
  
  	// 1 < mu <= population_size
  	if ((cgp_Para.mu < 1) || (cgp_Para.mu > cgp_Para.population_size))
  	{
  		println("mu (number of parents) must be greater than 1 and not greater than the populations size");
  		return
  	}
  	println("********* Beginning execution *********");
  }//END get_parameters()
 
  def read_plu( plufile:String) : Unit = {
    var file = Source.fromFile( plufile )
    var lines = file.getLines().flatMap( line =>  line.split(" +")).toArray
    
	   //输入个数ni
    cgp_Para.num_inputs = lines(1).toInt
	   //输出连接数no
	  cgp_Para.num_outputs = lines(3).toInt
	   //压缩真值表表项数
	  cgp_Para.num_products = lines(5).toInt
	  
		//compressed truth table
    for( i <- 0 until cgp_Para.num_products){
          for(j <- 0 until cgp_Para.num_inputs )
              cgp_Para.plu_inputs(i)(j) = lines( 6 + i*(cgp_Para.num_inputs + cgp_Para.num_outputs) +j ).toLong
          for(j <- 0 until cgp_Para.num_outputs)
              cgp_Para.plu_outputs(i)(j) = lines( 6 + cgp_Para.num_inputs + i*(cgp_Para.num_inputs + cgp_Para.num_outputs) +j).toLong
    }
    
    cgp_Para.num_nodes = cgp_Para.num_rows * cgp_Para.num_cols
    cgp_Para.num_genes = cgp_Para.num_genes_per_node * cgp_Para.num_nodes + cgp_Para.num_outputs
    cgp_Para.end_count = cgp_Para.num_inputs + cgp_Para.num_nodes
    
	//num_bits: the truth table items number(length)
  cgp_Para.num_bits=cgp_Para.pow2(cgp_Para.num_inputs)*cgp_Para.num_outputs;

  if (cgp_Para.num_inputs==2)
      cgp_Para.bit_width=4;
  else if (cgp_Para.num_inputs==3)
      cgp_Para.bit_width=8;
  else if (cgp_Para.num_inputs==4)
      cgp_Para.bit_width=16;
  else
      cgp_Para.bit_width=32;
  }//END read_plu()
  
  def write_cgp_info( command:String, plufile:String) : Unit = {
    val writer = new PrintWriter(new File("//home//pc//CGP//Output//cgp.txt"),"UTF-8")
     writer.write("The program is " + command + "\n")
     writer.write("The plu file is " + plufile + "\n")
     writer.write("num_points is " + cgp_Para.num_points + "\n")
     writer.write("population_size is " + cgp_Para.population_size + "\n")
     writer.write("mutation rate is " + cgp_Para.per_cent_mutate + "\n")
     writer.write("num_generations is " + cgp_Para.num_generations + "\n")
     writer.write("num_runs_total is " + cgp_Para.num_runs_total + "\n")
     writer.write("num_rows is " + cgp_Para.num_rows + "\n")
     writer.write("num_cols is " + cgp_Para.num_cols + "\n")
     writer.write("levels_back is " + cgp_Para.levels_back + "\n")
     writer.write("mu is " + cgp_Para.mu + "\n")
     
     for(i <- 0 until cgp_Para.MAX_NUM_FUNCTIONS){
        writer.write( cgp_Para.number(i) + "	" + cgp_Para.node_types(i) + "\n") 
     }
  
     writer.close()
  }
}