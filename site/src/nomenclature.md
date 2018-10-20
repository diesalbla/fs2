---
layout: page
title:  "Glossary"
section: "Glossary"
position: 5
---

- For simplicity, we use the symbol `S` as an abbreviation of `Stream`, we use `A`, `B`, `C` for type variables over data types, and we use `F`, `G`, `H` for type parameters of kind `* -> *`
- We ignore the extra type parameters which are added with a type annotation. 


### List-Like Stream operations

These operations behave similarly for `Streams` to how the functions with the same name for a `scala.immutable.List` would: 


| Method Name   | Type         | 
| ------------- |--------------|--------------|
| `append`  | `S[F, A] => S[F, A] => S[F, A]` | 
| `take`    | `S[F, A] => Int => S[F, A]`    | 
| `drop`    | `S[F, A] => Int => S[F, A]`    | 
| `takeRight` | `S[F, A] => Int => S[F, A]` | 
| `dropRight` | `S[F, A] => Int => S[F, A]` |
| `dropWhile` | `S[F, A] => (A => Bool) => S[F, A]` | 
| `either`   | `S[F, A] => S[F, B] => S[F, Either[A, B]]` | 
| `filter`   | `S[F, A] => (A => Bool) => S[F, A]` | 
| `find`   | `S[F, A] => (A => Bool) => S[F, A]` | 
| `fold`   | `S[F, A] => B => ( (B, A) => B ) => S[F, B]` | 
| `tail`   | `S[F, A] => S[F, A]` 


| Type          | Method Name  | Constraints  |
| ------------- |--------------|--------------|
|  `A => S[F, A]` | 

| `S[F,A] => B => S[F,B]` | `as` 
| `S[F, A] => S[F, Either[Th, A]]` | `attempt` 
| `S[F, A] => S[F, S[F, A]]`   | `broadcast` | 
| `S[F, A] => (A => B) => S[F,B]` | `collect` 
| `S[F, A] => A => S[F, A]` | `cons1` | 
| `S[F, A] => List[A] => S[F, A]` | `cons` | 
| `S[F, A] => (A => S[F, B]) => S[F, B]` | `evalMap` 
| `S[F, A] => (A => F[B]) => S[F, B]` | `evalMap` 
| `S[F, A] => (A => F[Unit]) => S[F, B]` | `evalTap` 
| `S[F, A] => (F ~> G) => S[G, A]` | `translate` 



| Type | is an alias for |
| -------------|--------------|
| `Pipe[F, A, B]` | `Stream[F, A] => Stream[F, B]`  | 
| `Sink[F, A]`    | `Stream[F, A] => Stream[F, Unit]` 
