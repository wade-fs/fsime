package com.wade.arity

// f'(x)=Im(f(x+i*h)/h) 
class Derivative(private val f: Function) : Function() {
    private val c = Complex()

    init {
        f.checkArity(1)
    }

    override fun eval(x: Double): Double {
        return f.eval(c.set(x, H))!!.im * INVH
    }

    override fun arity(): Int {
        return 1
    }

    companion object {
        private const val H = 1e-12
        private const val INVH = 1 / H
    }
}