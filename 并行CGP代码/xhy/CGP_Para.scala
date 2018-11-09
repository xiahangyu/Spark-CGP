package com.xhy

class CGP_Para extends Serializable{
   
     //define ANEALING
val MAX_NUM_ROWS:Int = 10
val MAX_NUM_COLS :Int= 1000
val MAX_NUM_INPUTS:Int=  256
val MAX_NUM_OUTPUTS:Int=  256
val MAX_NUM_PRODUCTS :Int= 1024
val MAX_NUM_NODES :Int=  MAX_NUM_ROWS*MAX_NUM_COLS
val MAX_NUM_GENES_PER_NODE :Int=   4
val MAX_NUM_GENES :Int=  MAX_NUM_GENES_PER_NODE*MAX_NUM_NODES
val MAX_OUTPUT_SIZE :Int=  MAX_NUM_INPUTS+MAX_NUM_NODES+MAX_NUM_OUTPUTS
val MAX_NUM_RUNS  :Int=  200

val MAX_NUM_CHROMOSOMES  :Int=    1000
val MAX_NUM_LETTERS     :Int=     100
val MAX_NUM_FUNCTIONS    :Int=   20
val MAXNUM  =  Int.MaxValue 

val MAX_NUM_POP :Int= 1024

//图上顶点的数量.
 var num_points:Int = 0

  //define Parameters
 var population_size:Int =0
 var per_cent_mutate:Double =0 
 var num_generations:Int =0
 
 var num_runs_total:Int =0
 var num_rows:Int=0
 var num_cols:Int=0
 var levels_back:Int=0
 var mu:Int=0
 
 var num_functions:Int=0
 var num_genes:Int =0
 var num_genes_per_node:Int=0
 var num_nodes:Int=0
 var number = new Array[Int](MAX_NUM_FUNCTIONS)
 var node_types = new Array[String](MAX_NUM_FUNCTIONS)
 var allowed_functions = new Array[Int](MAX_NUM_FUNCTIONS)
 
 //object logic circuits , trurh table
// global constants read from .plu file 
var        num_inputs:Int = 0
var        num_outputs:Int = 0
var				 num_products:Int	=0		//num of colomn. Compressed Truth table : 32bits per colomn
var        plu_inputs = Array.ofDim [Long] (MAX_NUM_PRODUCTS, MAX_NUM_INPUTS)
var        plu_outputs = Array.ofDim [Long] (MAX_NUM_PRODUCTS, MAX_NUM_OUTPUTS)

//calculated global constants in read_plu()
var				end_count:Int = 0			//num of all nodes except output nodes : end_count=num_inputs+num_nodes;
var				bit_width:Int = 0
//the truth table items number(length)
var				num_bits:Int = 0            //num_bits  = exp(2,num_inputs)*num_outputs    added by nfc .

var				gen_index :Int =0  

def pow2(x:Int) =  (1<<x)                                   // returns 2 to power x (x>=0)
def getbit( decimal:Long, nthbit:Int):Int = ( (decimal>>nthbit) & 1 ).toInt  // gets nth bit
}