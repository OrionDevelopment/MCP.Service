using Data.Core.Readers.Core;
using Data.Core.Readers.Mapping;
using Data.Core.Writers.Core;
using Data.Core.Writers.Mapping;
using Data.EFCore.Writer.Core;
using Data.EFCore.Writer.Mapping;
using Microsoft.Extensions.DependencyInjection;

namespace Data.EFCore.Extensions
{
    public static class ServiceCollectionExtensions
    {

        public static IServiceCollection AddMCMSDataServices(this IServiceCollection services)
        {
            services.AddTransient<IGameVersionReader, GameVersionWriter>();
            services.AddTransient<IGameVersionWriter, GameVersionWriter>();
            services.AddTransient<IReleaseReader, ReleaseWriter>();
            services.AddTransient<IReleaseWriter, ReleaseWriter>();
            services.AddTransient<IMappingTypeReader, MappingTypeWriter>();
            services.AddTransient<IMappingTypeWriter, MappingTypeWriter>();
            services.AddTransient<IClassComponentReader, ClassWriter>();
            services.AddTransient<IClassComponentWriter, ClassWriter>();
            services.AddTransient<IMethodComponentReader, MethodWriter>();
            services.AddTransient<IMethodComponentWriter, MethodWriter>();
            services.AddTransient<IFieldComponentReader, FieldWriter>();
            services.AddTransient<IFieldComponentWriter, FieldWriter>();
            services.AddTransient<IParameterComponentReader, ParameterWriter>();
            services.AddTransient<IParameterComponentWriter, ParameterWriter>();

            return services;
        }
    }
}
