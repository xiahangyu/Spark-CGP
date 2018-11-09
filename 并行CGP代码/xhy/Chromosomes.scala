package com.xhy


import com.xhy.Chromosome

class Chromosomes extends Serializable{
  var indivisuals:Array[Chromosome] =null
  var bestChromo_index:Int = 0
  var bestChromo_fitness:Int = 0
  var prebestChromo_fitness = 0
  var stick=0
  
}