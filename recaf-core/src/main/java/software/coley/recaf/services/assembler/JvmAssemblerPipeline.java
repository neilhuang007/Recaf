package software.coley.recaf.services.assembler;

import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.darknet.assembler.ast.ASTElement;
import me.darknet.assembler.compile.JavaClassRepresentation;
import me.darknet.assembler.compile.JvmCompiler;
import me.darknet.assembler.compile.JvmCompilerOptions;
import me.darknet.assembler.compile.analysis.EmptyMethodAnalysisLookup;
import me.darknet.assembler.compile.analysis.jvm.ValuedJvmAnalysisEngine;
import me.darknet.assembler.compiler.Compiler;
import me.darknet.assembler.compiler.CompilerOptions;
import me.darknet.assembler.compiler.InheritanceChecker;
import me.darknet.assembler.error.Result;
import me.darknet.assembler.parser.BytecodeFormat;
import me.darknet.assembler.parser.processor.ASTProcessor;
import me.darknet.assembler.printer.ClassPrinter;
import me.darknet.assembler.printer.JvmClassPrinter;
import software.coley.recaf.info.JvmClassInfo;
import software.coley.recaf.info.builder.JvmClassInfoBuilder;
import software.coley.recaf.path.AnnotationPathNode;
import software.coley.recaf.path.ClassMemberPathNode;
import software.coley.recaf.path.ClassPathNode;
import software.coley.recaf.path.PathNode;
import software.coley.recaf.services.inheritance.InheritanceGraph;
import software.coley.recaf.services.inheritance.InheritanceVertex;
import software.coley.recaf.util.JavaVersion;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

/**
 * JVM assembler pipeline implementation.
 *
 * @author Justus Garbe
 */
@ApplicationScoped
public class JvmAssemblerPipeline extends AbstractAssemblerPipeline<JvmClassInfo, JavaClassRepresentation> {
	public static final String SERVICE_ID = "jvm-assembler";

	private final ASTProcessor processor = new ASTProcessor(BytecodeFormat.JVM);
	private final InheritanceGraph inheritanceGraph;

	@Inject
	public JvmAssemblerPipeline(@Nonnull InheritanceGraph inheritanceGraph,
								@Nonnull AssemblerPipelineGeneralConfig generalConfig,
								@Nonnull JvmAssemblerPipelineConfig config) {
		super(generalConfig, config);
		this.inheritanceGraph = inheritanceGraph;
	}

	@Nonnull
	@Override
	public Result<List<ASTElement>> concreteParse(@Nonnull List<ASTElement> elements) {
		return processor.processAST(elements);
	}

	@Nonnull
	@Override
	public Result<JavaClassRepresentation> assemble(@Nonnull List<ASTElement> elements, @Nonnull PathNode<?> path) {
		return compile(elements, path);
	}

	@Nonnull
	@Override
	public Result<String> disassemble(@Nonnull ClassPathNode path) {
		return classPrinter(path).map(this::print);
	}

	@Nonnull
	@Override
	public Result<String> disassemble(@Nonnull ClassMemberPathNode path) {
		return memberPrinter(path).map(this::print);
	}

	@Nonnull
	@Override
	public Result<String> disassemble(@Nonnull AnnotationPathNode path) {
		return annotationPrinter(path).map(this::print);
	}

	@Nonnull
	@Override
	public JavaClassRepresentation getRepresentation(@Nonnull JvmClassInfo info) {
		return new JavaClassRepresentation(info.getBytecode(), EmptyMethodAnalysisLookup.instance());
	}

	@Nonnull
	@Override
	protected CompilerOptions<? extends CompilerOptions<?>> getCompilerOptions() {
		JvmCompilerOptions options = new JvmCompilerOptions();
		if (pipelineConfig.isValueAnalysisEnabled())
			options.engineProvider(ValuedJvmAnalysisEngine::new);
		return options;
	}

	@Nonnull
	@Override
	protected Compiler getCompiler() {
		return new JvmCompiler();
	}

	@Nonnull
	@Override
	protected InheritanceChecker getInheritanceChecker() {
		return new InheritanceChecker() {
			@Override
			public boolean isSubclassOf(String child, String parent) {
				InheritanceVertex childVertex = inheritanceGraph.getVertex(child);
				InheritanceVertex parentVertex = inheritanceGraph.getVertex(parent);

				if (childVertex == null || parentVertex == null) {
					return false;
				}

				return childVertex.isChildOf(parentVertex);
			}

			@Override
			public String getCommonSuperclass(String type1, String type2) {
				return inheritanceGraph.getCommon(type1, type2);
			}
		};
	}

	@Override
	protected int getClassVersion(@Nonnull JvmClassInfo info) {
		return info.getVersion() - JavaVersion.VERSION_OFFSET;
	}

	@Nonnull
	@Override
	public JvmClassInfo getClassInfo(@Nonnull JavaClassRepresentation representation) {
		return new JvmClassInfoBuilder(representation.classFile()).build();
	}

	@Nonnull
	@Override
	protected Result<ClassPrinter> classPrinter(@Nonnull ClassPathNode path) {
		try {
			return Result.ok(new JvmClassPrinter(new ByteArrayInputStream(path.getValue().asJvmClass().getBytecode())));
		} catch (IOException e) {
			return Result.exception(e);
		}
	}
}