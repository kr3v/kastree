package kastree.ast

import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtAnnotation
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry
import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.KtOperationExpression
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtSuperExpression
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeConstraint
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtWhenConditionInRange
import org.jetbrains.kotlin.psi.KtWhenConditionIsPattern
import org.jetbrains.kotlin.psi.KtWhenConditionWithExpression
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.KtWhileExpressionBase

sealed class Node {
    var tag: Any? = null
    abstract val element: PsiElement

    interface WithAnnotations {
        val anns: List<Modifier.AnnotationSet>
    }

    interface WithModifiers : WithAnnotations {
        val mods: List<Modifier>
        override val anns: List<Modifier.AnnotationSet> get() = mods.mapNotNull { it as? Modifier.AnnotationSet }
    }

    interface Entry : WithAnnotations {
        val pkg: Package?
        val imports: List<Import>
    }

    data class File(
        override val anns: List<Modifier.AnnotationSet>,
        override val pkg: Package?,
        override val imports: List<Import>,
        val decls: List<Decl>,
        override val element: KtFile
    ) : Node(), Entry

//    any usage?
//    data class Script(
//        override val anns: List<Modifier.AnnotationSet>,
//        override val pkg: Package?,
//        override val imports: List<Import>,
//        val exprs: List<Expr>
//    ) : Node(), Entry

    data class Package(
        override val mods: List<Modifier>,
        val names: List<String>,
        override val element: KtPackageDirective
    ) : Node(), WithModifiers

    data class Import(
        val names: List<String>,
        val wildcard: Boolean,
        val alias: String?,
        override val element: KtImportDirective
    ) : Node()

    sealed class Decl : Node() {
        abstract override val element: KtElement

        data class Structured(
            override val mods: List<Modifier>,
            val form: Form,
            val name: String,
            val typeParams: List<TypeParam>,
            val primaryConstructor: PrimaryConstructor?,
            val parentAnns: List<Modifier.AnnotationSet>,
            val parents: List<Parent>,
            val typeConstraints: List<TypeConstraint>,
            // TODO: Can include primary constructor
            val members: List<Decl>,
            override val element: KtClassOrObject
        ) : Decl(), WithModifiers {
            enum class Form {
                CLASS, ENUM_CLASS, INTERFACE, OBJECT, COMPANION_OBJECT
            }

            sealed class Parent : Node() {
                data class CallConstructor(
                    val type: TypeRef.Simple,
                    val typeArgs: List<Node.Type?>,
                    val args: List<ValueArg>,
                    val lambda: Expr.Call.TrailLambda?,
                    // TODO
                    override val element: KtSuperTypeListEntry
                ) : Parent()

                data class Type(
                    val type: TypeRef.Simple,
                    val by: Expr?,
                    // TODO
                    override val element: KtSuperTypeListEntry
                ) : Parent()
            }

            data class PrimaryConstructor(
                override val mods: List<Modifier>,
                val params: List<Func.Param>,
                override val element: KtPrimaryConstructor
            ) : Node(), WithModifiers
        }

        data class Init(
            val block: Block,
            override val element: KtAnonymousInitializer
        ) : Decl()

        data class Func(
            override val mods: List<Modifier>,
            val typeParams: List<TypeParam>,
            val receiverType: Type?,
            // Name not present on anonymous functions
            val name: String?,
            val paramTypeParams: List<TypeParam>,
            val params: List<Func.Param>,
            val type: Type?,
            val typeConstraints: List<TypeConstraint>,
            val body: Body?,
            override val element: KtNamedFunction
        ) : Decl(), WithModifiers {
            data class Param(
                override val mods: List<Modifier>,
                val readOnly: Boolean?,
                val name: String,
                // Type can be null for anon functions
                val type: Type?,
                val default: Expr?,
                override val element: KtParameter
            ) : Node(), WithModifiers

            sealed class Body : Node() {
                data class Block(val block: Node.Block) : Body() {
                    override val element: KtElement
                        get() = block.element
                }

                data class Expr(val expr: Node.Expr) : Body() {
                    override val element: KtElement
                        get() = expr.element
                }
            }
        }

        data class Property(
            override val mods: List<Modifier>,
            val readOnly: Boolean,
            val typeParams: List<TypeParam>,
            val receiverType: Type?,
            // Always at least one, more than one is destructuring, null is underscore in destructure
            val vars: List<Var?>,
            val typeConstraints: List<TypeConstraint>,
            val delegated: Boolean,
            val expr: Expr?,
            val accessors: Accessors?,
            // TODO (Either<...>)
            override val element: KtDeclaration
        ) : Decl(), WithModifiers {
            data class Var(
                val name: String,
                val type: Type?,
                // TODO (..?)
                override val element: KtDeclaration
            ) : Node()

            data class Accessors(
                val first: Accessor,
                val second: Accessor?
            ) : Node() {
                override val element: KtPropertyAccessor get() = first.element
            }

            sealed class Accessor : Node(), WithModifiers {
                abstract override val element: KtPropertyAccessor

                data class Get(
                    override val mods: List<Modifier>,
                    val type: Type?,
                    val body: Func.Body?,
                    override val element: KtPropertyAccessor
                ) : Accessor()

                data class Set(
                    override val mods: List<Modifier>,
                    val paramMods: List<Modifier>,
                    val paramName: String?,
                    val paramType: Type?,
                    val body: Func.Body?,
                    override val element: KtPropertyAccessor
                ) : Accessor()
            }
        }

        data class TypeAlias(
            override val mods: List<Modifier>,
            val name: String,
            val typeParams: List<TypeParam>,
            val type: Type,
            override val element: KtTypeAlias
        ) : Decl(), WithModifiers

        // TODO: secondary constructors
        data class Constructor(
            override val mods: List<Modifier>,
            val params: List<Func.Param>,
            val delegationCall: DelegationCall?,
            val block: Block?,
            override val element: KtConstructor<*>
        ) : Decl(), WithModifiers {
            data class DelegationCall(
                val target: DelegationTarget,
                val args: List<ValueArg>,
                override val element: KtConstructorDelegationCall
            ) : Node()

            enum class DelegationTarget { THIS, SUPER }
        }

        data class EnumEntry(
            override val mods: List<Modifier>,
            val name: String,
            val args: List<ValueArg>,
            val members: List<Decl>,
            override val element: KtEnumEntry
        ) : Decl(), WithModifiers
    }

    data class TypeParam(
        override val mods: List<Modifier>,
        val name: String,
        val type: TypeRef.Simple?,
        override val element: KtTypeParameter
    ) : Node(), WithModifiers

    data class TypeConstraint(
        override val anns: List<Modifier.AnnotationSet>,
        val name: String,
        val type: Type,
        override val element: KtTypeConstraint
    ) : Node(), WithAnnotations

    sealed class TypeRef : Node() {
        abstract override val element: KtElement

        data class Paren(
            override val mods: List<Modifier>,
            val type: TypeRef
        ) : TypeRef(), WithModifiers {
            override val element: KtElement = type.element
        }

        data class Func(
            val receiverType: Type?,
            val params: List<Param>,
            val type: Type
        ) : TypeRef() {
            override val element: KtElement = type.element

            data class Param(
                val name: String?,
                val type: Type
            ) : Node() {
                override val element: KtElement = type.element
            }
        }

        data class Simple(
            val pieces: List<Piece>,
            override val element: KtElement
        ) : TypeRef() {
            data class Piece(
                val name: String,
                // Null means any
                val typeParams: List<Type?>,
                override val element: KtElement
            ) : Node()
        }

        data class Nullable(
            val type: TypeRef,
            override val element: KtElement
        ) : TypeRef()

        data class Dynamic(
            val _unused_: Boolean = false,
            override val element: KtElement
        ) : TypeRef()
    }

    data class Type(
        override val mods: List<Modifier>,
        val ref: TypeRef
    ) : Node(), WithModifiers {
        override val element: KtElement get() = ref.element
    }

    data class ValueArg(
        val name: String?,
        val asterisk: Boolean,
        val expr: Expr,
        override val element: KtValueArgument
    ) : Node()

    sealed class Expr : Node() {
        abstract override val element: KtElement

        data class If(
            val expr: Expr,
            val body: Expr,
            val elseBody: Expr?,
            override val element: KtIfExpression
        ) : Expr()

        data class Try(
            val block: Block,
            val catches: List<Catch>,
            val finallyBlock: Block?,
            override val element: KtTryExpression
        ) : Expr() {
            data class Catch(
                override val anns: List<Modifier.AnnotationSet>,
                val varName: String,
                val varType: TypeRef.Simple,
                val block: Block,
                override val element: KtCatchClause
            ) : Node(), WithAnnotations
        }

        data class For(
            override val anns: List<Modifier.AnnotationSet>,
            // More than one means destructure, null means underscore
            val vars: List<Decl.Property.Var?>,
            val inExpr: Expr,
            val body: Expr,
            override val element: KtForExpression
        ) : Expr(), WithAnnotations

        data class While(
            val expr: Expr,
            val body: Expr,
            val doWhile: Boolean,
            override val element: KtWhileExpressionBase
        ) : Expr()

        data class BinaryOp(
            val lhs: Expr,
            val oper: Oper,
            val rhs: Expr
        ) : Expr() {
            override val element: KtBinaryExpression
                get() = oper.element

            sealed class Oper : Node() {
                abstract override val element: KtBinaryExpression

                data class Infix(
                    val str: String,
                    override val element: KtBinaryExpression
                ) : Oper()

                data class Token(
                    val token: BinaryOp.Token,
                    override val element: KtBinaryExpression
                ) : Oper()
            }

            enum class Token(val str: String) {
                MUL("*"), DIV("/"), MOD("%"), ADD("+"), SUB("-"),
                IN("in"), NOT_IN("!in"),
                GT(">"), GTE(">="), LT("<"), LTE("<="),
                EQ("=="), NEQ("!="),
                ASSN("="), MUL_ASSN("*="), DIV_ASSN("/="), MOD_ASSN("%="), ADD_ASSN("+="), SUB_ASSN("-="),
                OR("||"), AND("&&"), ELVIS("?:"), RANGE(""),
                SAFE("?")
            }
        }

        data class QualifiedOp(
            val lhs: Expr,
            val oper: With,
            val rhs: Expr
        ) : Expr() {
            override val element: KtQualifiedExpression
                get() = oper.element

            sealed class With(val str: String) : Node() {
                abstract override val element: KtQualifiedExpression

                data class Dot(override val element: KtDotQualifiedExpression) : With(".")
                data class Safe(override val element: KtSafeQualifiedExpression) : With("?.")
            }
        }

        data class UnaryOp(
            val expr: Expr,
            val oper: Oper,
            val prefix: Boolean,
            override val element: KtUnaryExpression
        ) : Expr() {
            data class Oper(val token: Token, override val element: KtSimpleNameExpression) : Node()
            enum class Token(val str: String) {
                NEG("-"), POS("+"), INC("++"), DEC("--"), NOT("!"), NULL_DEREF("!!")
            }
        }

        data class TypeOp(
            val lhs: Expr,
            val oper: Oper,
            val rhs: Type,
            override val element: KtOperationExpression
        ) : Expr() {
            data class Oper(val token: Token, override val element: KtSimpleNameExpression) : Node()
            enum class Token(val str: String) {
                AS("as"), AS_SAFE("as?"), COL(":"), IS("is"), NOT_IS("!is")
            }
        }

        sealed class DoubleColonRef : Expr() {
            abstract val recv: Recv?

            data class Callable(
                override val recv: Recv?,
                val name: String,
                override val element: KtCallableReferenceExpression
            ) : DoubleColonRef()

            data class Class(
                override val recv: Recv?,
                override val element: KtClassLiteralExpression
            ) : DoubleColonRef()

            sealed class Recv : Node() {
                data class Expr(
                    val expr: Node.Expr
                ) : Recv() {
                    override val element: KtElement
                        get() = expr.element
                }

                data class Type(
                    val type: TypeRef.Simple,
                    val questionMarks: Int,
                    override val element: KtExpression
                ) : Recv()
            }
        }

        data class Paren(
            val expr: Expr,
            override val element: KtParenthesizedExpression
        ) : Expr()

        data class StringTmpl(
            val elems: List<Elem>,
            val raw: Boolean,
            override val element: KtStringTemplateExpression
        ) : Expr() {
            sealed class Elem : Node() {
                data class Regular(
                    val str: String,
                    override val element: KtLiteralStringTemplateEntry
                ) : Elem()

                data class ShortTmpl(
                    val str: String,
                    override val element: KtSimpleNameStringTemplateEntry
                ) : Elem()

                data class UnicodeEsc(
                    val digits: String,
                    override val element: KtEscapeStringTemplateEntry
                ) : Elem()

                data class RegularEsc(
                    val char: Char,
                    override val element: KtEscapeStringTemplateEntry
                ) : Elem()

                data class LongTmpl(
                    val expr: Expr,
                    override val element: KtBlockStringTemplateEntry
                ) : Elem()
            }
        }

        data class Const(
            val value: String,
            val form: Form,
            override val element: KtConstantExpression
        ) : Expr() {
            enum class Form { BOOLEAN, CHAR, INT, FLOAT, NULL }
        }

        data class Brace(
            val params: List<Param>,
            val block: Block?,
            override val element: KtExpression
        ) : Expr() {
            data class Param(
                // Multiple means destructure, null means underscore
                val vars: List<Decl.Property.Var?>,
                val destructType: Type?,
                override val element: KtParameter
            ) : Expr()
        }

        data class This(
            val label: String?,
            override val element: KtThisExpression
        ) : Expr()

        data class Super(
            val typeArg: Type?,
            val label: String?,
            override val element: KtSuperExpression
        ) : Expr()

        data class When(
            val expr: Expr?,
            val entries: List<Entry>,
            override val element: KtWhenExpression
        ) : Expr() {
            data class Entry(
                val conds: List<Cond>,
                val body: Expr,
                override val element: KtWhenEntry
            ) : Node()

            sealed class Cond : Node() {
                data class Expr(
                    val expr: Node.Expr,
                    override val element: KtWhenConditionWithExpression
                ) : Cond()

                data class In(
                    val expr: Node.Expr,
                    val not: Boolean,
                    override val element: KtWhenConditionInRange
                ) : Cond()

                data class Is(
                    val type: Type,
                    val not: Boolean,
                    override val element: KtWhenConditionIsPattern
                ) : Cond()
            }
        }

        data class Object(
            val parents: List<Decl.Structured.Parent>,
            val members: List<Decl>,
            override val element: KtObjectLiteralExpression
        ) : Expr()

        data class Throw(
            val expr: Expr,
            override val element: KtThrowExpression
        ) : Expr()

        data class Return(
            val label: String?,
            val expr: Expr?,
            override val element: KtReturnExpression
        ) : Expr()

        data class Continue(
            val label: String?,
            override val element: KtContinueExpression
        ) : Expr()

        data class Break(
            val label: String?,
            override val element: KtBreakExpression
        ) : Expr()

        data class CollLit(
            val exprs: List<Expr>,
            override val element: KtCollectionLiteralExpression
        ) : Expr()

        data class Name(
            val name: String,
            override val element: KtSimpleNameExpression
        ) : Expr()

        data class Labeled(
            val label: String,
            val expr: Expr,
            override val element: KtLabeledExpression
        ) : Expr()

        data class Annotated(
            override val anns: List<Modifier.AnnotationSet>,
            val expr: Expr,
            override val element: KtAnnotatedExpression
        ) : Expr(), WithAnnotations

        data class Call(
            val expr: Expr,
            val typeArgs: List<Type?>,
            val args: List<ValueArg>,
            val lambda: TrailLambda?,
            override val element: KtCallExpression
        ) : Expr() {
            data class TrailLambda(
                override val anns: List<Modifier.AnnotationSet>,
                val label: String?,
                val func: Brace,
                override val element: KtLambdaArgument
            ) : Node(), WithAnnotations
        }

        data class ArrayAccess(
            val expr: Expr,
            val indices: List<Expr>,
            override val element: KtArrayAccessExpression
        ) : Expr()

        data class AnonFunc(
            val func: Decl.Func
        ) : Expr() {
            override val element: KtNamedFunction
                get() = func.element
        }

        // This is only present for when expressions and labeled expressions
        data class Property(
            val decl: Decl.Property,
            // TODO (Either<...>)
            override val element: KtDeclaration
        ) : Expr()
    }

    data class Block(
        val stmts: List<Stmt>,
        override val element: KtBlockExpression
    ) : Node()

    sealed class Stmt : Node() {
        data class Decl(val decl: Node.Decl) : Stmt() {
            // todo
            override val element: KtElement
                get() = decl.element
        }

        data class Expr(val expr: Node.Expr) : Stmt() {
            // todo
            override val element: KtElement
                get() = expr.element
        }
    }

    sealed class Modifier : Node() {
        data class AnnotationSet(
            val target: Target?,
            val anns: List<Annotation>,
            override val element: Either<KtAnnotation, KtAnnotationEntry>
        ) : Modifier() {
            enum class Target {
                FIELD, FILE, PROPERTY, GET, SET, RECEIVER, PARAM, SETPARAM, DELEGATE
            }

            data class Annotation(
                val names: List<String>,
                val typeArgs: List<Type>,
                val args: List<ValueArg>,
                override val element: KtAnnotationEntry
            ) : Node()
        }

        data class Lit(
            val keyword: Keyword,
            private val _element: PsiElement
        ) : Modifier() {
            override val element: KtElement
                get() = _element as KtElement
        }

        enum class Keyword {
            ABSTRACT, FINAL, OPEN, ANNOTATION, SEALED, DATA, OVERRIDE, LATEINIT, INNER,
            PRIVATE, PROTECTED, PUBLIC, INTERNAL,
            IN, OUT, NOINLINE, CROSSINLINE, VARARG, REIFIED,
            TAILREC, OPERATOR, INFIX, INLINE, EXTERNAL, SUSPEND, CONST,
            ACTUAL, EXPECT
        }
    }

    sealed class Extra : Node() {
        data class BlankLines(
            val count: Int,
            override val element: PsiWhiteSpace
        ) : Extra()

        data class Comment(
            val text: String,
            val startsLine: Boolean,
            val endsLine: Boolean,
            override val element: PsiComment
        ) : Extra()
    }

    sealed class Either<out L : KtElement, out R : KtElement> : KtElement {
        data class Left<L : KtElement>(val value: L) : Either<L, Nothing>(), KtElement by value
        data class Right<R : KtElement>(val value: R) : Either<Nothing, R>(), KtElement by value
    }
}