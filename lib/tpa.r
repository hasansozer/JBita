#R version 3.3.2 

# Implementation provided by
# Banks J., Garrabrant S.M., Huber M.L., Perizzolo A.
# in their paper titled "Using TPA to count linear extensi-ons" 
# published in Journal of Discrete Algorithms, 2018.

#necessary to install the library for the first time
#install.packages('gtools', repos='http://cran.us.r-project.org')

library(gtools)
library(Matrix)
  
chain.step <- function(state,beta,i,c1,c2,posetmatrix) {
# Takes one step in the Karzanov-Khatchyan chain using randomness in i, c1, c2
# Assumes that home state is the identity permutation
# Line 2 of Algorithm 5.1
d <- i + 1 - state[i]
# Line 3 of Algorithm 5.1
if ((state[i] <= length(state)) && (state[i+1] <= length(state)))
posetflag <- posetmatrix[state[i],state[i+1]]
else
posetflag <- 0
if ( (c1 == 1) && (posetflag == 0) &&
(d <= ceiling(beta)) && ((d < beta) || (c2 == 1))) {
13
a <- state[i+1]; state[i+1] <- state[i]; state[i] <- a
}
return(state)
}
    
bounding.chain.step <- function(cstate,beta,i,c1,c2,posetmatrix) {
# Based on Algorithm 5.2
# Here cstate is a matrix with two rows, the first is the underlying state,
# while the second is the bounding state
n <- dim(cstate)[2]
# Line 1
if (cstate[2,i+1] == cstate[1,i]) c3 <- 1 - c1
else c3 <- c1
# Line 2 & 3
cstate[1,] <- chain.step(cstate[1,],beta,i,c3,c2,posetmatrix)
cstate[2,] <- chain.step(cstate[2,],beta,i,c1,c2,posetmatrix)
# Line 4 through 6
if (cstate[2,n] == (n+1))
cstate[2,n] <- 1 + sum(cstate[2,] <= n)
#Line 7
return(cstate)
}
generate <- function(t,beta,posetmatrix) {
n <- dim(posetmatrix)[1]; beta <- min(beta,n-1)
# Line 1
sigma <- 1:n; B <- rep(n+1,n)
# Line 2 thorugh 4
for (item in 1:(n - ceiling(beta)))
B[item+ceiling(beta)] <- item
B0 <- B
cstate <- matrix(c(sigma,B),byrow=TRUE,nrow=2)
# Line 5 through 8
i <- floor(runif(t)*(n-1))+1;c1 <- rbinom(t,1,1/2)
c2 <- rbinom(t,1,1+beta-ceiling(beta))
for (s in 1:t) {
cstate <- bounding.chain.step(cstate,beta,i[s],c1[s],c2[s],posetmatrix)
}
# Line 9
if (sum(cstate[2,] < (n+1)) == n) return(cstate[2,])
# Line 11-15
else {
cstate[1,] <- generate(2*t,beta,posetmatrix)
14
cstate[2,] <- B0
for (s in 1:t)
cstate <- bounding.chain.step(cstate,beta,i[s],c1[s],c2[s],posetmatrix)
}
return(cstate[1,])
}
    
approximate.sample <- function(n,beta,posetmatrix,tvdist) {
# Generates an approximate sample from the target distribution
x <- 1:n
n <- length(x)
t <- 10*n^3*log(n)*log(1/tvdist)
for (i in 1:t) {
i <- runif(1)*(n-1)+1
c1 <- rbinom(1,1,1/2)
c2 <- rbinom(1,1,1+beta-ceiling(beta))
x <- chain.step(x,beta,i,c1,c2,posetmatrix)
}
return(x)
}
checksum <- function(x) {
checksum <- 0
n <- length(x)
for (i in n:1) {
onespot <- which(x == 1)
checksum <- checksum + factorial(i-1)*(onespot-1)
x <- x[-onespot] - 1
}
return(checksum+1)
}
    
count.perfect.linear.extensions <- function(n = 4,beta = 4,posetmatrix,trials = 100) {
# Generates a number of linear extensions, then counts the results
results <- rep(0,factorial(n))
# Burnin to an approximate sample
x <- 1:n
n <- length(x)
# Take data
for (i in 1:trials) {
15
x <- generate(1,beta,posetmatrix)
cs <- checksum(x)
results[cs] <- results[cs] + 1
}
return(results/trials)
}
    
tpa.count.linear.extensions <- function(r,posetmatrix) {
# Algorithm 6.1
# Returns an estimate of the number of linear
# extensions consistant with posetmatrix
require(Matrix)
n <- dim(posetmatrix)[1]
# Line 1
k <- 0
# Line 2 through 12
for (i in 1:r) {
beta <- n - 1; k <- k - 1
while (beta > 0) {
k <- k + 1
x <- generate(1,beta,posetmatrix)
xinv <- invPerm(x)
betastep <- rep(0,n); y <- rep(0,n)
for (j in 1:n) {
y[j] <- runif(1)*((1+beta-ceiling(beta))*(xinv[j]-j == ceiling(beta))+
((xinv[j]-j) < ceiling(beta)))
betastep[j] <- (xinv[j]-j-1+y[j])*(xinv[j]-j > 0)
}
beta <- max(betastep)
# cat(" X: ",x,"\n X^{-1}: ",xinv,"\n Y: ",y,"\n betastep: ",betastep,"\n beta: ",beta,"\n")
}
}
cat(" Estimate: [",exp((k-2*sqrt(k))/r),",",exp((k+2*sqrt(k))/r),"]\n")
return(exp(k/r))
}
    
tpa.approximation <- function(posetmatrix,epsilon,delta) {
# Gives an $(\epsilon,\delta)$-ras for the number of posets
r1 <- ceiling(2*log(2/delta))
a1 <- tpa.count.linear.extensions(r1,posetmatrix)
16
a1 <- log(a1)
r2 <- ceiling(2*(a1+sqrt(a1)+2)*
(log(1+epsilon)^2-log(1+epsilon)^3)^(-1)*log(4/delta))
a2 <- tpa.count.linear.extensions(r2,posetmatrix)
return(a2)
}
    
brute.force.count.linear.extensions <- function(posets) {
# Counts the number of linear extensions of a poset by direct
# ennumeration of all n! permutations and checking each to see
# if it is a linear extension
#
# The poset is given as an n by n matrix whose (i,j)th entry
# is the indicator function of $i \preceq j$
require(gtools)
n <- dim(posets)[1]
A <- permutations(n,n)
nfact <- nrow(A)
le.flag <- rep(1,nfact)
count <- 0
for (i in 1:nfact) {
for (a in 1:(n-1))
for (b in (a+1):n) {
le.flag[i] <- le.flag[i]*(1-posets[A[i,b],A[i,a]])
}
count <- count + le.flag[i]
}
return(count)
}
    
   
poset1 <- matrix(c(1,0,1,0,0,1,0,1,0,0,1,0,0,0,0,1),byrow=TRUE,ncol=4)
poset2 <- matrix(c(1,0,1,1,1,1,1,1, 0,1,0,1,0,1,1,1, 0,0,1,0,1,1,0,1,0,0,0,1,0,1,1,1, 0,0,0,0,1,0,0,0, 0,0,0,0,0,1,0,1,0,0,0,0,0,0,1,1, 0,0,0,0,0,0,0,1),byrow=TRUE,ncol=8)
poset3 <- t(matrix(c(1,1,0,1,0,1,0,1,0,0,1,1,0,0,0,1),nrow=4))
poset4 <- t(matrix(c(1,0,1,1,1,1, 0,1,0,1,1,1, 0,0,1,0,1,0,0,0,0,1,1,1, 0,0,0,0,1,0, 0,0,0,0,0,1),nrow=6))
poset5 <- matrix(c(1,0,0,0,0,0,0,1,0,0,1,0,0,0,0,1),byrow=TRUE,ncol=4)

print("Results for poset1")
print(brute.force.count.linear.extensions(poset1))
print(tpa.approximation(poset1,1,0.1))

print("Results for poset5")
print(brute.force.count.linear.extensions(poset5))
print(tpa.approximation(poset5,1,0.1))