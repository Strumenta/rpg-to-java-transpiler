package com.strumenta.rpgtojava.intermediateast

import com.strumenta.kolasu.model.Node

abstract class GType : Node()

object GStringType : GType()
object GIntegerType : GType()
object GDecimalType : GType()
